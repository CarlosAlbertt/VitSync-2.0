import { useMutation } from "@tanstack/react-query";
import { BadgeCheck } from "lucide-react";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { request } from "@/shared/api/client";
import { useSession } from "@/shared/api/session";
import type { Me } from "@/shared/api/types";
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

type Editable = Pick<Me, "phone" | "bloodType" | "allergies" | "emergencyContact">;

function age(birthDate: string) {
  const birth = new Date(birthDate);
  const now = new Date();
  return now.getFullYear() - birth.getFullYear() - (now < new Date(now.getFullYear(), birth.getMonth(), birth.getDate()) ? 1 : 0);
}

export function ProfilePage() {
  const { user, setUser } = useSession();
  const [size, setSize] = useState<TextSize>(getTextSize);
  const { register, handleSubmit } = useForm<Editable>({
    defaultValues: { phone: user?.phone, bloodType: user?.bloodType, allergies: user?.allergies, emergencyContact: user?.emergencyContact },
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

  const identity = [
    ["Nombre", user.firstName],
    ["Apellidos", user.lastName],
    ["Fecha de nacimiento", `${formatDate(user.birthDate)} (${age(user.birthDate)} años)`],
    ["NIF", "•••••678Z"],
    ["Email", user.email],
    ["Tarjeta sanitaria", "VLCN 0000 0000 00"],
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
              <p className="mt-2">
                <Badge tone="success">
                  <BadgeCheck className="size-3.5" aria-hidden />
                  Identidad verificada
                </Badge>
              </p>
            </div>
            <Button variant="soft" onClick={() => demo("Cambiar la foto")}>
              Cambiar foto
            </Button>
          </section>

          <Panel title="Datos personales">
            <dl className="grid gap-x-6 gap-y-4 sm:grid-cols-2">
              {identity.map(([term, value]) => (
                <div key={term}>
                  <dt className="text-sm text-ink-soft">{term}</dt>
                  <dd className="font-semibold">{value}</dd>
                </div>
              ))}
            </dl>
            <p className="mt-4 text-sm text-ink-soft">Para cambiar tu nombre, NIF o fecha de nacimiento, contacta con atención al paciente: hay que comprobar tu identidad.</p>
          </Panel>

          <Panel title="Contacto y salud">
            <form onSubmit={handleSubmit((values) => save.mutate(values))} className="grid gap-4">
              <div className="grid gap-4 sm:grid-cols-2">
                <TextField label="Teléfono" type="tel" autoComplete="tel" {...register("phone")} />
                <TextField label="Grupo sanguíneo" {...register("bloodType")} />
              </div>
              <TextAreaField label="Alergias" {...register("allergies")} />
              <TextField label="Contacto de emergencia" {...register("emergencyContact")} />
              <Button type="submit" className="justify-self-start" disabled={save.isPending}>
                {save.isPending ? "Guardando…" : "Guardar cambios"}
              </Button>
            </form>
          </Panel>

          <Panel title="Dirección">
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

          <Panel title="Idioma">
            <SelectField label="Idioma de la aplicación" defaultValue="es" onChange={() => demo("Cambiar el idioma")}>
              <option value="es">Español</option>
              <option value="ca">Valencià</option>
              <option value="en">English</option>
            </SelectField>
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
