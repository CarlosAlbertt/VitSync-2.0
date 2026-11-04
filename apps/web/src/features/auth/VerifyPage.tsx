import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Navigate, useLocation, useNavigate } from "react-router";
import { request } from "@/shared/api/client";
import { env } from "@/shared/config/env";
import { errorMessage } from "@/shared/lib/errors";
import { toast } from "@/shared/lib/toast";
import { Button } from "@/shared/ui/button";
import { FormError } from "@/shared/ui/feedback";
import { TextField } from "@/shared/ui/field";
import { AuthCard } from "./AuthCard";
import { verifySchema, type VerifyInput } from "./schemas";

export function VerifyPage() {
  const navigate = useNavigate();
  const email = (useLocation().state as { email?: string } | null)?.email;
  const { register, handleSubmit, formState: { errors } } = useForm<VerifyInput>({ resolver: zodResolver(verifySchema) });

  const verify = useMutation({
    mutationFn: ({ code }: VerifyInput) => request("/auth/verify", { method: "POST", body: { email, code } }),
    onSuccess: () => {
      toast.success("Cuenta verificada. Ya puedes entrar.");
      navigate("/entrar");
    },
  });

  if (!email) return <Navigate to="/registro" replace />;

  return (
    <AuthCard title="Verifica tu email" intro={<>Hemos enviado un código de 6 dígitos a {email}.</>}>
      <form noValidate onSubmit={handleSubmit((values) => verify.mutate(values))} className="grid gap-4">
        {verify.isError && <FormError>{errorMessage(verify.error)}</FormError>}
        <TextField
          label="Código"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          hint={env.VITE_API_MOCKS === "true" ? "Demostración: usa 123456." : undefined}
          error={errors.code?.message}
          {...register("code")}
        />
        <Button type="submit" disabled={verify.isPending}>
          {verify.isPending ? "Comprobando…" : "Verificar"}
        </Button>
      </form>
    </AuthCard>
  );
}
