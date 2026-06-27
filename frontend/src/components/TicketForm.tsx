import { useState, type ChangeEvent, type FormEvent } from "react";
import {
  PRIORITY_OPTIONS,
  STATUS_OPTIONS,
  type TicketFormValues,
} from "../ticketConfig";

type TicketFormProps = {
  className?: string;
  heading?: string;
  initialValues: TicketFormValues;
  resetOnSubmit?: boolean;
  submitLabel: string;
  onSubmit: (values: TicketFormValues) => Promise<void> | void;
  onCancel?: () => void;
};

function TicketForm({
  className,
  heading,
  initialValues,
  resetOnSubmit = false,
  submitLabel,
  onSubmit,
  onCancel,
}: TicketFormProps) {
  const [values, setValues] = useState(() => ({ ...initialValues }));
  const [error, setError] = useState("");
  const [isSubmitting, setIsSubmitting] = useState(false);

  function handleChange(
    event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>
  ) {
    const { name, value } = event.target;
    if (error !== "") {
      setError("");
    }
    setValues((current) => ({ ...current, [name]: value }));
  }

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError("");
    setIsSubmitting(true);

    try {
      await onSubmit(values);
      if (resetOnSubmit) {
        setValues({ ...initialValues });
      }
    } catch (submitError) {
      setError(
        submitError instanceof Error
          ? submitError.message
          : "Could not save ticket changes."
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className={className} onSubmit={handleSubmit}>
      {heading && <h2>{heading}</h2>}
      {error && <p role="alert">{error}</p>}
      <input name="title" placeholder="Title" value={values.title} onChange={handleChange} />
      <textarea
        name="description"
        placeholder="Description"
        value={values.description}
        onChange={handleChange}
      />
      <select name="status" value={values.status} onChange={handleChange}>
        {STATUS_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <select name="priority" value={values.priority} onChange={handleChange}>
        {PRIORITY_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <input
        name="assignee"
        placeholder="Assignee"
        value={values.assignee}
        onChange={handleChange}
      />
      <button type="submit" disabled={isSubmitting}>
        {isSubmitting ? "Saving..." : submitLabel}
      </button>
      {onCancel && (
        <button type="button" disabled={isSubmitting} onClick={onCancel}>
          Cancel
        </button>
      )}
    </form>
  );
}

export default TicketForm;
