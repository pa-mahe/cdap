import isNil from 'lodash/isNil';

export function getEpochDateString(params) {
    if (!isNil(params)) {
      if (isNaN(params.value)) {
        return "—";
      } else {
        const date = new Date(params.value * 1000);
        return date.toDateString();
      }
    }
    return "—";
  }