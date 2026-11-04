import { toast } from "./toast";

/** Para los botones del prototipo que aún no hacen nada: lo dicen en vez de fallar en silencio. */
export const demo = (action: string) => toast.info(`${action}: disponible próximamente. Esto es una demostración.`);
