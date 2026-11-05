import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router";
import { request } from "@/shared/api/client";
import type { Registration } from "@/shared/api/types";
import { errorMessage } from "@/shared/lib/errors";
import { Button } from "@/shared/ui/button";
import { FormError } from "@/shared/ui/feedback";
import { TextField } from "@/shared/ui/field";
import { AuthCard } from "./AuthCard";
import { registerSchema, type RegisterInput } from "./schemas";

// El cuerpo que pide el contrato. El consentimiento se exige en el formulario, pero no viaja.
const toRequest = ({ firstName, lastName, nif, birthDate, email, password }: RegisterInput) => ({
  firstName: firstName.trim(),
  lastName: lastName.trim(),
  nif: nif.trim().toUpperCase(),
  birthDate,
  email: email.trim(),
  password,
});

export function RegisterPage() {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm<RegisterInput>({ resolver: zodResolver(registerSchema) });

  const signUp = useMutation({
    mutationFn: (values: RegisterInput) => request<Registration>("/auth/register", { method: "POST", body: toRequest(values) }),
    onSuccess: (_, values) => navigate("/verificar", { state: { email: values.email } }),
  });

  return (
    <AuthCard title="Crea tu cuenta" intro="Unos pocos datos y tendrás tu área de salud lista." wide>
      <form noValidate onSubmit={handleSubmit((values) => signUp.mutate(values))} className="grid gap-4">
        {signUp.isError && <FormError>{errorMessage(signUp.error)}</FormError>}
        <h2 className="text-sm font-bold text-accent-strong">Datos personales</h2>
        <div className="grid gap-4 sm:grid-cols-2">
          <TextField label="Nombre" autoComplete="given-name" error={errors.firstName?.message} {...register("firstName")} />
          <TextField label="Apellidos" autoComplete="family-name" error={errors.lastName?.message} {...register("lastName")} />
          <TextField label="NIF o NIE" autoComplete="off" error={errors.nif?.message} {...register("nif")} />
          <TextField label="Fecha de nacimiento" type="date" autoComplete="bday" error={errors.birthDate?.message} {...register("birthDate")} />
        </div>
        <h2 className="mt-2 text-sm font-bold text-accent-strong">Datos de cuenta</h2>
        <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} />
        <TextField
          label="Contraseña"
          type="password"
          autoComplete="new-password"
          hint="Mínimo 12 caracteres. Mejor una frase que una palabra."
          error={errors.password?.message}
          {...register("password")}
        />
        <div>
          <label className="flex cursor-pointer items-start gap-3 text-sm">
            <input type="checkbox" className="mt-0.5 size-4 accent-[var(--accent)]" aria-invalid={Boolean(errors.acceptsHealthData)} {...register("acceptsHealthData")} />
            <span>Acepto el tratamiento de mis datos de salud para gestionar mi atención médica.</span>
          </label>
          {errors.acceptsHealthData && <p className="mt-1 text-[0.82rem] font-medium text-danger">{errors.acceptsHealthData.message}</p>}
        </div>
        <Button type="submit" disabled={signUp.isPending}>
          {signUp.isPending ? "Creando cuenta…" : "Crear cuenta"}
        </Button>
      </form>
      <p className="mt-5 text-center text-sm text-ink-soft">
        ¿Ya tienes cuenta?{" "}
        <Link to="/entrar" className="font-semibold text-accent hover:underline">
          Inicia sesión
        </Link>
      </p>
    </AuthCard>
  );
}
