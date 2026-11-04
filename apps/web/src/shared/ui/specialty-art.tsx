// Las ilustraciones de public/specialties se llaman como en la v1; estas especialidades cambian de nombre.
const ALIASES: Record<string, string> = {
  "medicina-familiar": "medicina-general",
  psiquiatria: "psicologia",
  radiologia: "diagnostico-imagen",
};

const FALLBACK = "/specialties/medicina-general.webp";

export function SpecialtyArt({ slug, className = "" }: { slug: string; className?: string }) {
  return (
    <img
      src={`/specialties/${ALIASES[slug] ?? slug}.webp`}
      alt=""
      loading="lazy"
      onError={(e) => {
        if (!e.currentTarget.src.endsWith(FALLBACK)) e.currentTarget.src = FALLBACK;
      }}
      className={`bg-white object-cover ${className}`}
    />
  );
}
