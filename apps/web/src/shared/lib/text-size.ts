// Tamaño del texto elegido en Mis datos. Es una preferencia del dispositivo, no del paciente.
export type TextSize = "normal" | "large" | "xlarge";

const KEY = "vitsync-text-size";
const SCALE: Record<TextSize, string> = { normal: "100%", large: "112.5%", xlarge: "125%" };

export function getTextSize(): TextSize {
  try {
    const value = localStorage.getItem(KEY);
    return value === "large" || value === "xlarge" ? value : "normal";
  } catch {
    return "normal";
  }
}

export function setTextSize(size: TextSize) {
  document.documentElement.style.fontSize = SCALE[size];
  try {
    localStorage.setItem(KEY, size);
  } catch {
    // Sin almacenamiento la preferencia dura hasta recargar.
  }
}
