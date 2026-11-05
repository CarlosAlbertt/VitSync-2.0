import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useForm } from "react-hook-form";
import { Link, Navigate, useLocation, useNavigate } from "react-router";
import { request } from "@/shared/api/client";
import { errorMessage, hasCode } from "@/shared/lib/errors";
import { toast } from "@/shared/lib/toast";
import { Button } from "@/shared/ui/button";
import { FormError } from "@/shared/ui/feedback";
import { TextField } from "@/shared/ui/field";
import { AuthCard } from "./AuthCard";
import { codeHint } from "./codeHint";
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
        {verify.isError && (
          <FormError>
            {hasCode(verify.error, "TOO_MANY_ATTEMPTS") ? (
              <>
                Has superado los intentos con este código.{" "}
                <Link to="/entrar" className="underline">
                  Vuelve a entrar
                </Link>{" "}
                y te enviaremos otro.
              </>
            ) : (
              errorMessage(verify.error)
            )}
          </FormError>
        )}
        <TextField
          label="Código"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          hint={codeHint()}
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
