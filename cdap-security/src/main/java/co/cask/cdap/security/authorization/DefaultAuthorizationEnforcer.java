/*
 * Copyright © 2016-2017 Cask Data, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package co.cask.cdap.security.authorization;

import co.cask.cdap.common.conf.CConfiguration;
import co.cask.cdap.common.conf.Constants;
import co.cask.cdap.common.lang.ClassLoaders;
import co.cask.cdap.proto.element.EntityType;
import co.cask.cdap.proto.id.EntityId;
import co.cask.cdap.proto.id.NamespaceId;
import co.cask.cdap.proto.id.NamespacedEntityId;
import co.cask.cdap.proto.security.Action;
import co.cask.cdap.proto.security.AuthorizationPrivilege;
import co.cask.cdap.proto.security.Principal;
import co.cask.cdap.proto.security.VisibilityRequest;
import co.cask.cdap.security.spi.authorization.AuthorizationEnforcer;
import co.cask.cdap.security.spi.authorization.UnauthorizedException;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.commons.lang.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * An implementation of {@link AuthorizationEnforcer} that runs on the master. It calls the authorizer directly to
 * enforce authorization policies.
 */
@Singleton
public class DefaultAuthorizationEnforcer extends AbstractAuthorizationEnforcer {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAuthorizationEnforcer.class);

    private final AuthorizerInstantiator authorizerInstantiator;
    private final boolean cacheEnabled;
    private final LoadingCache<AuthorizationPrivilege, Boolean> authPolicyCache;
    private final LoadingCache<RemoteAuthorizationEnforcer.VisibilityKey, Boolean> visibilityCache;


    @Nullable
    private final Principal masterUser;
    private final int logTimeTakenAsWarn;

    @Inject
    DefaultAuthorizationEnforcer(CConfiguration cConf, AuthorizerInstantiator authorizerInstantiator) {
        super(cConf);
        this.authorizerInstantiator = authorizerInstantiator;
        String masterUserName = AuthorizationUtil.getEffectiveMasterUser(cConf);
        this.masterUser = masterUserName == null ? null : new Principal(masterUserName, Principal.PrincipalType.USER);
        this.logTimeTakenAsWarn = cConf.getInt(Constants.Security.Authorization.EXTENSION_OPERATION_TIME_WARN_THRESHOLD);
        int cacheTTLSecs = cConf.getInt(Constants.Security.Authorization.CACHE_TTL_SECS);
        int cacheMaxEntries = cConf.getInt(Constants.Security.Authorization.CACHE_MAX_ENTRIES);
        // Cache can be disabled by setting the number of entries to <= 0
        this.cacheEnabled = cacheMaxEntries > 0;

        int perCacheSize = cacheMaxEntries / 2 + 1;
        authPolicyCache = CacheBuilder.newBuilder()
                .expireAfterWrite(cacheTTLSecs, TimeUnit.SECONDS)
                .maximumSize(perCacheSize)
                .build(new CacheLoader<AuthorizationPrivilege, Boolean>() {
                    @Override
                    @ParametersAreNonnullByDefault
                    public Boolean load(AuthorizationPrivilege authorizationPrivilege) throws Exception {
                        LOG.trace("Cache miss for {}", authorizationPrivilege);
                        return doEnforce(authorizationPrivilege);
                    }
                });

        visibilityCache = CacheBuilder.newBuilder()
                .expireAfterAccess(cacheTTLSecs, TimeUnit.SECONDS)
                .maximumSize(perCacheSize)
                .build(new CacheLoader<RemoteAuthorizationEnforcer.VisibilityKey, Boolean>() {
                    @Override
                    @ParametersAreNonnullByDefault
                    public Boolean load(RemoteAuthorizationEnforcer.VisibilityKey key) throws Exception {
                        LOG.trace("Cache miss for {}", key);
                        return !authorizerInstantiator.get().isVisible(Collections.singleton(key.getEntityId()), key.getPrincipal()).isEmpty();
                    }
                });
    }

    @Override
    public void enforce(EntityId entity, Principal principal, Action action) throws Exception {
        if (!isSecurityAuthorizationEnabled()) {
            return;
        }

        if (isAccessingSystemNSAsMasterUser(entity, principal) || isEnforcingOnSamePrincipalId(entity, principal)) {
            return;
        }

        AuthorizationPrivilege authorizationPrivilege = new AuthorizationPrivilege(principal, entity, action);
        boolean allowed = cacheEnabled ? authPolicyCache.get(authorizationPrivilege) : doEnforce(authorizationPrivilege);
        if (!allowed) {
            throw new UnauthorizedException(principal, action, entity);
        }

//    doEnforce(entity, principal, Collections.singleton(action));
    }

    @Override
    public Set<? extends EntityId> isVisible(Set<? extends EntityId> entityIds, Principal principal) throws Exception {
        if (!isSecurityAuthorizationEnabled()) {
            return entityIds;
        }

        Set<EntityId> visibleEntities = new HashSet<>();
        // filter out entity id which is in system namespace and principal is the master user
        for (EntityId entityId : entityIds) {
            if (isAccessingSystemNSAsMasterUser(entityId, principal) || isEnforcingOnSamePrincipalId(entityId, principal)) {
                visibleEntities.add(entityId);
            }
        }

        Set<? extends EntityId> difference = Sets.difference(entityIds, visibleEntities);
        LOG.trace("Checking visibility of {} for principal {}.", difference, principal);
        // create new stopwatch instance every time enforce is called since the DefaultAuthorizationEnforcer is binded as
        // singleton we don't want the stopwatch instance to get re-used across multiple calls.
        StopWatch watch = new StopWatch();
        watch.start();
        Set<? extends EntityId> moreVisibleEntities;
        try {
            AuthorizerClassLoader authorizerClassLoader = authorizerInstantiator.getAuthorizerClassLoader();
            ClassLoaders.setContextClassLoader(authorizerClassLoader);

            if (cacheEnabled) {
                Iterable<RemoteAuthorizationEnforcer.VisibilityKey> visibilityKeys = RemoteAuthorizationEnforcer.toVisibilityKeys(principal, difference);
                Map<RemoteAuthorizationEnforcer.VisibilityKey, Boolean> visibilityMap = visibilityCache.getAll(visibilityKeys);
                moreVisibleEntities = RemoteAuthorizationEnforcer.toEntityIds(Maps.filterEntries(visibilityMap, RemoteAuthorizationEnforcer.VISIBILITY_KEYS_FILTER).keySet());
            } else {
                moreVisibleEntities = authorizerInstantiator.get().isVisible(difference, principal);
            }
        } finally {
            watch.stop();
            long timeTaken = watch.getTime();
            String logLine = "Checked visibility of {} for principal {}. Time spent in visibility check was {} ms.";
            if (timeTaken > logTimeTakenAsWarn) {
                LOG.warn(logLine, difference, principal, timeTaken);
            } else {
                LOG.trace(logLine, difference, principal, timeTaken);
            }
        }
        visibleEntities.addAll(moreVisibleEntities);
        LOG.trace("Getting {} as visible entities", visibleEntities);
        return Collections.unmodifiableSet(visibleEntities);
    }

    private boolean doEnforce(AuthorizationPrivilege authorizationPrivilege) throws Exception {
        EntityId entity = authorizationPrivilege.getEntity();
        Principal principal = authorizationPrivilege.getPrincipal();
        Action action = authorizationPrivilege.getAction();

        try {
            doEnforce(entity, principal, Collections.singleton(action));
            return true;
        } catch (UnauthorizedException ex) {
            return false;
        }
    }

    private void doEnforce(EntityId entity, Principal principal, Set<Action> actions) throws Exception {
        // bypass the check when the principal is the master user and the entity is in the system namespace
        if (isAccessingSystemNSAsMasterUser(entity, principal) || isEnforcingOnSamePrincipalId(entity, principal)) {
            return;
        }
        LOG.trace("Enforcing actions {} on {} for principal {}.", actions, entity, principal);
        // create new stopwatch instance every time enforce is called since the DefaultAuthorizationEnforcer is binded as
        // singleton we don't want the stopwatch instance to get re-used across multiple calls.
        StopWatch watch = new StopWatch();
        watch.start();
        try {
            AuthorizerClassLoader authorizerClassLoader = authorizerInstantiator.getAuthorizerClassLoader();
            ClassLoaders.setContextClassLoader(authorizerClassLoader);
            authorizerInstantiator.get().enforce(entity, principal, actions);
        } finally {
            watch.stop();
            long timeTaken = watch.getTime();
            String logLine = "Enforced actions {} on {} for principal {}. Time spent in enforcement was {} ms.";
            if (timeTaken > logTimeTakenAsWarn) {
                LOG.warn(logLine, actions, entity, principal, watch.getTime());
            } else {
                LOG.trace(logLine, actions, entity, principal, watch.getTime());
            }
        }
    }


    private boolean isAccessingSystemNSAsMasterUser(EntityId entityId, Principal principal) {
        return entityId instanceof NamespacedEntityId &&
                ((NamespacedEntityId) entityId).getNamespaceId().equals(NamespaceId.SYSTEM) && principal.equals(masterUser);
    }

    private boolean isEnforcingOnSamePrincipalId(EntityId entityId, Principal principal) {
        return entityId.getEntityType().equals(EntityType.KERBEROSPRINCIPAL) &&
                principal.getName().equals(entityId.getEntityName());
    }
}
