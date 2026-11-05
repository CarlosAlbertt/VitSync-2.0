import type { Doctor, Facility, Me, Report, Specialty } from "@/shared/api/types";

// Datos sintéticos: ninguna persona, colegiado ni centro de esta lista es real.

export const DEMO_PASSWORD = "Demostracion-2026";
export const DEMO_EMAIL = "paciente@vitsync.test";
export const DEMO_CODE = "123456";

export const demoUser: Me = {
  id: "0190a0a0-0000-7000-8000-000000000001",
  email: DEMO_EMAIL,
  role: "PATIENT",
  firstName: "Lucía",
  lastName: "Ferrer Navarro",
  phone: "+34600000000",
  locale: "es-ES",
  emailVerified: true,
};

const spec = (slug: string, name: string, kind: Specialty["kind"], description: string): Specialty => ({
  publicId: `spec-${slug}`,
  slug,
  name,
  kind,
  description,
  doctorCount: 0,
});

export const specialties: Specialty[] = [
  spec("medicina-familiar", "Medicina familiar", "GENERAL", "Tu primer punto de contacto: revisiones, seguimiento y derivaciones."),
  spec("cardiologia", "Cardiología", "MEDICAL", "Corazón y sistema circulatorio, de la prevención al seguimiento."),
  spec("dermatologia", "Dermatología", "MEDICAL", "Piel, cabello y uñas. Revisión de lunares y tratamientos."),
  spec("pediatria", "Pediatría", "MEDICAL", "Salud y desarrollo desde el nacimiento hasta la adolescencia."),
  spec("ginecologia", "Ginecología y obstetricia", "SURGICAL", "Salud de la mujer, embarazo y revisiones periódicas."),
  spec("traumatologia", "Traumatología", "SURGICAL", "Huesos, articulaciones y lesiones deportivas."),
  spec("oftalmologia", "Oftalmología", "SURGICAL", "Revisión de la vista y patologías oculares."),
  spec("neurologia", "Neurología", "MEDICAL", "Migrañas, trastornos del sueño y sistema nervioso."),
  spec("psiquiatria", "Psiquiatría", "MEDICAL", "Salud mental con seguimiento continuado."),
  spec("radiologia", "Radiología", "DIAGNOSTIC", "Pruebas de imagen: radiografía, ecografía y resonancia."),
  spec("analisis-clinicos", "Análisis clínicos", "DIAGNOSTIC", "Analíticas de sangre y orina con resultados en tu perfil."),
  spec("endocrinologia", "Endocrinología", "MEDICAL", "Tiroides, diabetes y metabolismo."),
];

export const facilities: Facility[] = [
  { publicId: "fac-turia", name: "Centro Jardín del Turia", address: "Paseo de la Ribera 12", city: "València", phone: "+34 960 000 010" },
  { publicId: "fac-albufera", name: "Clínica Albufera", address: "Avenida del Puerto 88", city: "València", phone: "+34 960 000 020" },
  { publicId: "fac-retiro", name: "Consultas Retiro", address: "Calle de los Tilos 5", city: "Madrid", phone: "+34 910 000 030" },
  { publicId: "fac-barceloneta", name: "Espai Mediterrani", address: "Passeig de la Mar 21", city: "Barcelona", phone: "+34 930 000 040" },
];

const seed: [string, string, string, string, string[]][] = [
  ["Inés", "Soler Martí", "medicina-familiar", "Acompaña a familias enteras desde hace quince años. Cree en explicar las cosas sin prisa.", ["fac-turia", "fac-albufera"]],
  ["Tomás", "Aguirre Vidal", "medicina-familiar", "Especial interés en enfermedades crónicas y prevención.", ["fac-retiro"]],
  ["Marta", "Bellver Ros", "cardiologia", "Cardiología preventiva y rehabilitación tras un evento cardíaco.", ["fac-turia"]],
  ["Hugo", "Requena Sanz", "cardiologia", "Ecocardiografía e insuficiencia cardíaca.", ["fac-retiro", "fac-barceloneta"]],
  ["Clara", "Montoro Gil", "dermatologia", "Dermatoscopia digital y seguimiento de lunares.", ["fac-albufera"]],
  ["Pau", "Esteve Roig", "pediatria", "Consulta del niño sano y asma infantil.", ["fac-barceloneta"]],
  ["Nuria", "Cano Ibáñez", "pediatria", "Lactancia, alimentación y desarrollo en los primeros años.", ["fac-turia"]],
  ["Elena", "Vidal Ortuño", "ginecologia", "Seguimiento del embarazo y salud pélvica.", ["fac-turia", "fac-retiro"]],
  ["Álvaro", "Pastor Llorens", "traumatologia", "Rodilla, hombro y lesiones del deporte.", ["fac-albufera", "fac-retiro"]],
  ["Sara", "Quiles Domingo", "oftalmologia", "Retina y cirugía de cataratas.", ["fac-barceloneta"]],
  ["Diego", "Marín Tena", "neurologia", "Cefaleas y trastornos del sueño.", ["fac-retiro"]],
  ["Laia", "Ferrando Pons", "psiquiatria", "Ansiedad, depresión y terapia combinada.", ["fac-turia", "fac-barceloneta"]],
  ["Rubén", "Sancho Beltrán", "radiologia", "Resonancia musculoesquelética.", ["fac-albufera"]],
  ["Carmen", "Ibars Lluch", "analisis-clinicos", "Hematología y bioquímica clínica.", ["fac-turia"]],
  ["Óscar", "Navarro Mir", "endocrinologia", "Diabetes tipo 1 y tecnología para el control de la glucosa.", ["fac-retiro"]],
];

export const doctors: Doctor[] = seed.map(([firstName, lastName, slug, bio, facilityIds], i) => {
  const specialty = specialties.find((s) => s.slug === slug)!;
  specialty.doctorCount += 1;
  return {
    publicId: `doc-${i + 1}`,
    firstName,
    lastName,
    bio,
    licenseNumber: `46/${String(10000 + i * 137)}`,
    specialty: { publicId: specialty.publicId, slug, name: specialty.name },
    facilities: facilityIds.map((id) => {
      const f = facilities.find((x) => x.publicId === id)!;
      return { publicId: f.publicId, name: f.name, city: f.city };
    }),
  };
});

export const reports: Report[] = [
  { publicId: "rep-1", title: "Analítica general de sangre", kind: "LAB", issuedOn: "2026-09-22", doctorName: "Carmen Ibars Lluch", fileCount: 1 },
  { publicId: "rep-2", title: "Revisión de lunares", kind: "DIAGNOSIS", issuedOn: "2026-07-03", doctorName: "Clara Montoro Gil", fileCount: 2 },
  { publicId: "rep-3", title: "Ecografía de rodilla izquierda", kind: "IMAGING", issuedOn: "2026-05-18", doctorName: "Rubén Sancho Beltrán", fileCount: 3 },
  { publicId: "rep-4", title: "Receta: tratamiento de migraña", kind: "PRESCRIPTION", issuedOn: "2026-03-09", doctorName: "Diego Marín Tena", fileCount: 1 },
  { publicId: "rep-5", title: "Electrocardiograma de control", kind: "DIAGNOSIS", issuedOn: "2025-11-27", doctorName: "Marta Bellver Ros", fileCount: 1 },
];
