import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { useNavigate } from "react-router";
import { request } from "@/shared/api/client";
import { errorMessage, hasCode } from "@/shared/lib/errors";
import { toast } from "@/shared/lib/toast";
import { Button } from "@/shared/ui/button";
import { FormError } from "@/shared/ui/feedback";
import { TextField } from "@/shared/ui/field";
import { AuthCard } from "./AuthCard";
import { codeHint } from "./codeHint";
import { recoverRequestSchema, recoverSchema, type RecoverInput, type RecoverRequestInput } from "./schemas";

function RequestStep({ onSent }: { onSent: (email: string) => void }) {
  const { register, handleSubmit, formState: { errors } } = useForm<RecoverRequestInput>({ resolver: zodResolver(recoverRequestSchema) });
  const send = useMutation({
    mutationFn: (body: RecoverRequestInput) => request("/auth/password-reset/request", { method: "POST", body }),
    onSuccess: (_, { email }) => onSent(email),
  });
  return (
    <form noValidate onSubmit={handleSubmit((values) => send.mutate(values))} className="grid gap-4">
      {send.isError && <FormError>{errorMessage(send.error)}</FormError>}
      <TextField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register("email")} />
      <Button type="submit" disabled={send.isPending}>
        Enviar código
      </Button>
    </form>
  );
}

function ResetStep({ email, onRestart }: { email: string; onRestart: () => void }) {
  const navigate = useNavigate();
  const { register, handleSubmit, formState: { errors } } = useForm<RecoverInput>({ resolver: zodResolver(recoverSchema) });
  const reset = useMutation({
    mutationFn: (values: RecoverInput) => request("/auth/password-reset", { method: "POST", body: { ...values, email } }),
    onSuccess: () => {
      toast.success("Contraseña cambiada. Se han cerrado tus sesiones abiertas.");
      navigate("/entrar");
    },
  });
  return (
    <form noValidate onSubmit={handleSubmit((values) => reset.mutate(values))} className="grid gap-4">
      {reset.isError && (
        <FormError>
          {errorMessage(reset.error)}{" "}
          {hasCode(reset.error, "TOO_MANY_ATTEMPTS") && (
            <button type="button" onClick={onRestart} className="cursor-pointer underline">
              Pedir otro código
            </button>
          )}
        </FormError>
      )}
      {/* La respuesta es la misma exista o no el email: no revela qué cuentas hay. */}
      <p className="text-sm text-ink-soft">Si {email} tiene cuenta, recibirás un código en unos segundos.</p>
      <TextField
        label="Código"
        inputMode="numeric"
        autoComplete="one-time-code"
        maxLength={6}
        hint={codeHint()}
        error={errors.code?.message}
        {...register("code")}
      />
      <TextField label="Contraseña nueva" type="password" autoComplete="new-password" hint="Mínimo 12 caracteres. Mejor una frase que una palabra." error={errors.newPassword?.message} {...register("newPassword")} />
      <Button type="submit" disabled={reset.isPending}>
        Cambiar contraseña
      </Button>
    </form>
  );
}

export function RecoverPage() {
  const [email, setEmail] = useState<string | null>(null);
  return <AuthCard title="Recuperar contraseña">{email ? <ResetStep email={email} onRestart={() => setEmail(null)} /> : <RequestStep onSent={setEmail} />}</AuthCard>;
}
