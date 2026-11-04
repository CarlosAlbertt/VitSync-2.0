import { ApiError } from "@/shared/api/client";

// La API devuelve code y texto en inglés; el usuario lee español. El front decide por el code.
const MESSAGES: Record<string, string> = {
  INVALID_CREDENTIALS: "El email o la contraseña no son correctos.",
  ACCOUNT_NOT_VERIFIED: "Tu cuenta aún no está verificada. Revisa tu correo.",
  ACCOUNT_ALREADY_EXISTS: "No hemos podido crear la cuenta con esos datos. Si ya tienes una, entra con ella.",
  PASSWORD_BREACHED: "Esa contraseña aparece en filtraciones conocidas. Elige otra.",
  INVALID_CODE: "El código no es correcto o ha caducado.",
  APPOINTMENT_SLOT_TAKEN: "Alguien acaba de reservar ese hueco. Te mostramos los que siguen libres.",
  APPOINTMENT_IN_THE_PAST: "No se puede reservar una cita en el pasado.",
  APPOINTMENT_NOT_CANCELLABLE: "Esta cita ya no se puede cancelar.",
  ACCESS_DENIED: "No tienes permiso para hacer esto.",
  VALIDATION_FAILED: "Revisa los campos marcados.",
};

export function errorMessage(error: unknown) {
  if (error instanceof ApiError) {
    return MESSAGES[error.code] ?? "Algo ha fallado. Inténtalo de nuevo en unos minutos.";
  }
  return "No hemos podido conectar. Comprueba tu conexión.";
}

export function hasCode(error: unknown, code: string) {
  return error instanceof ApiError && error.code === code;
}
