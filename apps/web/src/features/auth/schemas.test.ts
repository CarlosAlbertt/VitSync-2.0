import { describe, expect, it } from "vitest";
import { isValidNif, registerSchema } from "./schemas";

describe("isValidNif", () => {
  it.each(["12345678Z", "00000000T", "x1234567l", " 12345678z "])("acepta %s", (nif) => {
    expect(isValidNif(nif)).toBe(true);
  });

  it.each(["12345678A", "1234567Z", "ABCDEFGHI", ""])("rechaza %j", (nif) => {
    expect(isValidNif(nif)).toBe(false);
  });
});

describe("registerSchema", () => {
  const valid = {
    firstName: "Lucía",
    lastName: "Ferrer",
    nif: "12345678Z",
    birthDate: "1991-03-14",
    email: "lucia@vitsync.test",
    password: "Contraseña-Larga-1",
    acceptsHealthData: true,
  };

  it("acepta un registro correcto", () => {
    expect(registerSchema.safeParse(valid).success).toBe(true);
  });

  // La misma regla que el backend: longitud, no composición.
  it("exige 12 caracteres como mínimo y acepta una frase sin símbolos", () => {
    expect(registerSchema.safeParse({ ...valid, password: "Corta-1" }).success).toBe(false);
    expect(registerSchema.safeParse({ ...valid, password: "una frase bastante larga" }).success).toBe(true);
  });

  it("rechaza más de 72 bytes, el límite de BCrypt, aunque sean menos caracteres", () => {
    // "ñ" ocupa 2 bytes en UTF-8: 40 caracteres son 80 bytes.
    expect(registerSchema.safeParse({ ...valid, password: "ñ".repeat(40) }).success).toBe(false);
    expect(registerSchema.safeParse({ ...valid, password: "ñ".repeat(36) }).success).toBe(true);
  });

  it("no acepta una fecha futura", () => {
    expect(registerSchema.safeParse({ ...valid, birthDate: "2999-01-01" }).success).toBe(false);
  });

  it("exige el consentimiento de datos de salud", () => {
    expect(registerSchema.safeParse({ ...valid, acceptsHealthData: false }).success).toBe(false);
  });
});
