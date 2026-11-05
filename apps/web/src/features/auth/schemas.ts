import { z } from "zod";

const NIF_LETTERS = "TRWAGMYFPDXBNJZSQVHLCKE";

/** NIF o NIE con su letra de control. El backend vuelve a validarlo: es la autoridad. */
export function isValidNif(value: string) {
  const nif = value.trim().toUpperCase();
  const match = /^([0-9]{8}|[XYZ][0-9]{7})([A-Z])$/.exec(nif);
  if (!match) return false;
  const digits = match[1]!.replace("X", "0").replace("Y", "1").replace("Z", "2");
  return NIF_LETTERS[Number(digits) % 23] === match[2];
}

const email = z.email("Escribe un email válido.");
// La misma regla que el backend (@ValidPassword): 12 caracteres o más y como mucho 72 bytes, el
// límite de BCrypt. Sin reglas de composición: protege la longitud, y el backend además rechaza
// las contraseñas que aparecen en filtraciones conocidas.
const password = z
  .string()
  .min(12, "Mínimo 12 caracteres.")
  .refine((value) => new TextEncoder().encode(value).length <= 72, "Demasiado larga: como mucho 72 bytes.");

export const loginSchema = z.object({
  email,
  password: z.string().min(1, "Escribe tu contraseña."),
});

export const registerSchema = z.object({
  firstName: z.string().trim().min(2, "Escribe tu nombre."),
  lastName: z.string().trim().min(2, "Escribe tus apellidos."),
  nif: z.string().refine(isValidNif, "Ese NIF no es válido: revisa la letra."),
  birthDate: z.iso
    .date("Escribe tu fecha de nacimiento.")
    .refine((d) => new Date(d) < new Date(), "La fecha no puede ser futura."),
  email,
  password,
  acceptsHealthData: z.literal(true, "Necesitamos tu consentimiento para tratar datos de salud."),
});

export const verifySchema = z.object({
  code: z.string().regex(/^\d{6}$/, "El código tiene 6 dígitos."),
});

export const recoverRequestSchema = z.object({ email });

export const recoverSchema = z.object({
  code: z.string().regex(/^\d{6}$/, "El código tiene 6 dígitos."),
  newPassword: password,
});

export type LoginInput = z.infer<typeof loginSchema>;
export type RegisterInput = z.infer<typeof registerSchema>;
export type VerifyInput = z.infer<typeof verifySchema>;
export type RecoverRequestInput = z.infer<typeof recoverRequestSchema>;
export type RecoverInput = z.infer<typeof recoverSchema>;
