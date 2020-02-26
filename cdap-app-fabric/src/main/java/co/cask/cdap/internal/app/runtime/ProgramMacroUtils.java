package co.cask.cdap.internal.app.runtime;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import co.cask.cdap.api.macro.MacroFunction;
import co.cask.cdap.api.macro.Macros;
import co.cask.cdap.api.plugin.Plugin;
import co.cask.cdap.app.program.ProgramDescriptor;

public final class ProgramMacroUtils {
  private static final Logger LOG = LoggerFactory.getLogger(ProgramMacroUtils.class);
  private static final String SECURE_FUNCTION_NAME = "secure";


  public static Set<String> getSecureMacrosInProgramRun(ProgramDescriptor programDescriptor,
      Map<String, String> args) {
    
    if (programDescriptor == null) {
      return Collections.EMPTY_SET;
    }
    
    Set<String> secureMacroKeys = new HashSet<String>();
    Map<String, Plugin> plugins = programDescriptor.getApplicationSpecification().getPlugins();
    for (Map.Entry<String, Plugin> entry : plugins.entrySet()) {
      Plugin plugin = entry.getValue();
      Macros macros = plugin.getProperties().getMacros();
      Set<MacroFunction> macroFns = macros.getMacroFunctions();
      for (MacroFunction macroFn : macroFns) {
        if (!SECURE_FUNCTION_NAME.equals(macroFn.getFunctionName())) {
          continue;
        }
        if (macroFn.getArguments().size() > 1) {
          continue;
        }
        secureMacroKeys.add(macroFn.getArguments().get(0));
      }
    }
    secureMacroKeys.addAll(getSecureMacrosFromArgs(args));
    return secureMacroKeys;
  }

  private static Set<String> getSecureMacrosFromArgs(Map<String, String> args) {
    if (args == null) {
      return Collections.EMPTY_SET;
    }
    Set<String> secureMacroArgs = new HashSet<String>();
    for (Map.Entry<String, String> entry : args.entrySet()) {
      String val = entry.getValue();
      String secureFnCheck = "${" + SECURE_FUNCTION_NAME + "(";
      if (val.startsWith(secureFnCheck)) {
        String val1 = val.substring(secureFnCheck.length());
        if (val1.indexOf(")") > 0) {
          String key = val1.substring(0, val1.indexOf(")"));
          secureMacroArgs.add(key);
        }
      }
    }
    return secureMacroArgs;
  }
}
