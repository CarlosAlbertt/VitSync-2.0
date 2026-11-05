import { useMutation } from "@tanstack/react-query";
import { BadgeCheck, MailWarning } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { request } from "@/shared/api/client";
import { useSession } from "@/shared/api/session";
import type { Me } from "@/shared/api/types";
import { clinicalProfile } from "@/shared/demo/patient";
import { demo } from "@/shared/lib/demo";
import { errorMessage } from "@/shared/lib/errors";
import { formatDate, fullName } from "@/shared/lib/format";
import { getTextSize, setTextSize, type TextSize } from "@/shared/lib/text-size";
import { toast } from "@/shared/lib/toast";
import { Avatar } from "@/shared/ui/avatar";
import { Badge } from "@/shared/ui/badge";
import { Button } from "@/shared/ui/button";
import { SelectField, TextAreaField, TextField } from "@/shared/ui/field";
import { PageHeader, Panel } from "@/shared/ui/panel";
import { Segmented } from "@/shared/ui/segmented";

// Lo que PATCH /api/v1/me deja cambiar. Un teléfono vacío lo borra.
type Editable = { firstName: string; lastName: string; phone: string; locale: string };

const ROLE: Record<Me["role"], string> = { PATIENT: "Paciente", DOCTOR: "Profesional sanitario", ADMIN: "Administración" };

function age(birthDate: string) {
  const birth = new Date(birthDate);
  const now = new Date();
  return now.getFullYear() - birth.getFullYear() - (now < new Date(now.getFullYear(), birth.getMonth(), birth.getDate()) ? 1 : 0);
}

function ExampleBadge() {
  return <Badge>Ejemplo</Badge>;
}

export function ProfilePage() {
  const { user, setUser } = useSession();
  const [size, setSize] = useState<TextSize>(getTextSize);
  const {
    register,
    handleSubmit,
    formState: { errors, isDirty },
  } = useForm<Editable>({
    // values (y no defaultValues): el formulario se rehace cuando la API devuelve el usuario guardado.
    values: user ? { firstName: user.firstName, lastName: user.lastName, phone: user.phone ?? "", locale: user.locale } : undefined,
  });

  const save = useMutation({
    mutationFn: (body: Editable) => request<Me>("/me", { method: "PATCH", body }),
    onSuccess: (updated) => {
      setUser(updated);
      toast.success("Datos guardados.");
    },
    onError: (error) => toast.error(errorMessage(error)),
  });

  if (!user) return null;

  // Aún no los devuelve la API: llegarán con el módulo clínico. Mientras, se enseñan de ejemplo.
  const identity = [
    ["NIF", clinicalProfile.nif],
    ["Fecha de nacimiento", `${formatDate(clinicalProfile.birthDate)} (${age(clinicalProfile.birthDate)} años)`],
    ["Tarjeta sanitaria", clinicalProfile.healthCard],
  ];

  return (
    <>
      <PageHeader title="Mis datos" description="Tu información personal y de contacto, y cómo quieres ver VitSync." />

      <div className="grid grid-cols-1 gap-6 xl:grid-cols-[minmax(0,1fr)_22rem]">
        <div className="grid min-w-0 content-start gap-6">
          <section className="card flex flex-wrap items-center gap-5 p-6 md:p-8">
            <Avatar person={user} className="size-20 text-2xl" />
            <div className="min-w-0 flex-1">
              <h2 className="text-2xl font-black tracking-tight">{fullName(user)}</h2>
              <p className="text-ink-soft">{user.email}</p>
              <p className="mt-2 flex flex-wrap gap-2">
                <Badge tone="info">{ROLE[user.role]}</Badge>
                {user.emailVerified ? (
                  <Badge tone="success">
                    <BadgeCheck className="size-3.5" aria-hidden />
                    Email verificado
                  </Badge>
                ) : (
                  <Badge tone="warning">
                    <MailWarning className="size-3.5" aria-hidden />
                    Email sin verificar
                  </Badge>
                )}
              </p>
            </div>
            <Button variant="soft" onClick={() => demo("Cambiar la foto")}>
              Cambiar foto
            </Button>
          </section>

          <Panel title="Datos personales">
            <form noValidate onSubmit={handleSubmit((values) => save.mutate(values))} className="grid gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField
                  label="Nombre"
                  autoComplete="given-name"
                  error={errors.firstName?.message}
                  {...register("firstName", { required: "Escribe tu nombre.", maxLength: { value: 100, message: "Máximo 100 caracteres." } })}
                />
                <TextField
                  label="Apellidos"
                  autoComplete="family-name"
                  error={errors.lastName?.message}
                  {...register("lastName", { required: "Escribe tus apellidos.", maxLength: { value: 100, message: "Máximo 100 caracteres." } })}
                />
                <TextField
                  label="Teléfono"
                  type="tel"
                  autoComplete="tel"
                  hint="Opcional. Con prefijo si no es español, p. ej. +351…"
                  error={errors.phone?.message}
                  {...register("phone", { pattern: { value: /^$|^\+?[0-9]{9,15}$/, message: "Entre 9 y 15 dígitos, sin espacios." } })}
                />
                <SelectField label="Idioma" {...register("locale")}>
                  <option value="es-ES">Español</option>
                  <option value="ca-ES">Valencià</option>
                  <option value="en-GB">English</option>
                </SelectField>
              </div>
              <p className="text-sm text-ink-soft">El email, el NIF y el tipo de cuenta no se cambian aquí: hay que volver a comprobar tu identidad.</p>
              <Button type="submit" className="justify-self-start" disabled={!isDirty || save.isPending}>
                {save.isPending ? "Guardando…" : "Guardar cambios"}
              </Button>
            </form>
          </Panel>

          <Panel title="Identificación" actions={<ExampleBadge />}>
            <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-3">
              {identity.map(([term, value]) => (
                <div key={term}>
                  <dt className="text-sm text-ink-soft">{term}</dt>
                  <dd className="font-semibold">{value}</dd>
                </div>
              ))}
            </dl>
          </Panel>

          <Panel title="Datos de salud" actions={<ExampleBadge />}>
            <div className="grid gap-4">
              <TextField label="Grupo sanguíneo" defaultValue={clinicalProfile.bloodType} />
              <TextAreaField label="Alergias" defaultValue={clinicalProfile.allergies} />
              <TextField label="Contacto de emergencia" defaultValue={clinicalProfile.emergencyContact} />
            </div>
            <p className="mt-3 text-sm text-ink-soft">Se guardarán cifrados cuando exista el módulo clínico. De momento es un ejemplo.</p>
            <Button variant="soft" className="mt-4" onClick={() => demo("Guardar los datos de salud")}>
              Guardar datos de salud
            </Button>
          </Panel>

          <Panel title="Dirección" actions={<ExampleBadge />}>
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="sm:col-span-2">
                <TextField label="Dirección" defaultValue="Calle ficticia 1, 3.º B" />
              </div>
              <TextField label="Código postal" defaultValue="46000" />
              <TextField label="Ciudad" defaultValue="València" />
            </div>
            <Button variant="soft" className="mt-4" onClick={() => demo("Guardar la dirección")}>
              Guardar dirección
            </Button>
          </Panel>
        </div>

        <div className="grid content-start gap-6">
          <Panel title="Tamaño del texto">
            <Segmented
              label="Tamaño del texto"
              value={size}
              onChange={(value) => {
                setSize(value);
                setTextSize(value);
              }}
              options={[
                { value: "normal", label: "Normal" },
                { value: "large", label: "Grande" },
                { value: "xlarge", label: "Muy grande" },
              ]}
            />
            <p className="mt-3 text-sm text-ink-soft">Se aplica en este dispositivo.</p>
          </Panel>

          <Panel title="Centro de referencia">
            <p className="font-bold">Centro Jardín del Turia</p>
            <p className="text-sm text-ink-soft">Paseo de la Ribera 12, València</p>
            <Button variant="soft" size="sm" className="mt-4 w-full" onClick={() => demo("Cambiar de centro")}>
              Cambiar centro
            </Button>
          </Panel>
        </div>
      </div>
    </>
  );
}
