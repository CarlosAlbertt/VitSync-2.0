import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation, useNavigate } from "react-router";
import { request } from "@/shared/api/client";
import { useSession } from "@/shared/api/session";
import type { Session } from "@/shared/api/types";
import { env } from "@/shared/config/env";
import { errorMessage, hasCode } from "@/shared/lib/errors";
import { Button } from "@/shared/ui/button";
import { FormError } from "@/shared/ui/feedback";
import { TextField } from "@/shared/ui/field";
import { AuthCard } from "./AuthCard";
import { loginSchema, type LoginInput } from "./schemas";

// Solo con la API simulada: en un entorno real, estas credenciales no existen.
const DEMO = { email: "paciente@vitsync.test", password: "Demostracion-2026" };

export function LoginPage() {
  const { status, signIn } = useSession();
  const navigate = useNavigate();
  const location = useLocation();
  const from = (location.state as { from?: string } | null)?.from ?? "/panel";

  const { register, handleSubmit, formState: { errors } } = useForm<LoginInput>({ resolver: zodResolver(loginSchema) });

  const login = useMutation({
    mutationFn: (body: LoginInput) => request<Session>("/auth/login", { method: "POST", body }),
    onSuccess: (session) => {
      signIn(session);
      navigate(from, { replace: true });
    },
  });

  if (status === "authenticated" && !login.isSuccess) return <Navigate to={from} replace />;

  return (
    <AuthCard title="Inicia sesión" intro="Accede a tu área de salud.">
      <form noValidate onSubmit={handleSubmit((values) => login.mutate(values))} className="grid gap-4">
        {login.isError && (
          <FormError>
            {errorMessage(login.error)}{" "}
            {hasCode(login.error, "ACCOUNT_NOT_VERIFIED") && (
              <Link to="/verificar" className="underline">
                Verificar ahora
              </Link>
            )}
          </FormError>
        )}
        <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} />
        <TextField label="Contraseña" type="password" autoComplete="current-password" error={errors.password?.message} {...register("password")} />
        <Link to="/recuperar" className="justify-self-end text-sm font-medium text-accent hover:underline">
          ¿Has olvidado tu contraseña?
        </Link>
        <Button type="submit" disabled={login.isPending}>
          {login.isPending ? "Entrando…" : "Entrar"}
        </Button>
      </form>

      {env.VITE_API_MOCKS === "true" && (
        <Button variant="soft" className="mt-3 w-full" disabled={login.isPending} onClick={() => login.mutate(DEMO)}>
          Usar cuenta de demostración
        </Button>
      )}

      <p className="mt-5 text-center text-sm text-ink-soft">
        ¿No tienes cuenta?{" "}
        <Link to="/registro" className="font-semibold text-accent hover:underline">
          Crear cuenta
        </Link>
      </p>
    </AuthCard>
  );
}
