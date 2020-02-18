import isNil from "lodash/isNil";

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

export function copyToClipboard(clipboardText) {
  const textArea = document.createElement("textarea");
  textArea.value = clipboardText;
  document.body.appendChild(textArea);
  textArea.select();
  document.execCommand("copy");
  document.body.removeChild(textArea);
}
