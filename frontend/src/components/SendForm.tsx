import { useState, type ChangeEvent, type FormEvent } from "react";
import {
  DISCIPLINE_OPTIONS,
  GRADE_SYSTEM_OPTIONS,
  ROPE_SEND_STYLE_OPTIONS,
  SOURCE_APP_OPTIONS,
  type SendFormValues,
} from "../sendConfig";

type SendFormProps = {
  className?: string;
  heading?: string;
  initialValues: SendFormValues;
  resetOnSubmit?: boolean;
  submitLabel: string;
  onSubmit: (values: SendFormValues) => Promise<void> | void;
  onCancel?: () => void;
};

function SendForm({
  className,
  heading,
  initialValues,
  resetOnSubmit = false,
  submitLabel,
  onSubmit,
  onCancel,
}: SendFormProps) {
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
          : "Could not save send changes."
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <form className={className} onSubmit={handleSubmit}>
      {heading && <h2>{heading}</h2>}
      {error && <p role="alert">{error}</p>}
      <input
        name="climbName"
        placeholder="Climb name"
        required
        value={values.climbName}
        onChange={handleChange}
      />
      <input
        name="location"
        placeholder="Location"
        value={values.location}
        onChange={handleChange}
      />
      <select name="discipline" value={values.discipline} onChange={handleChange}>
        {DISCIPLINE_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <input
        name="grade"
        placeholder="Grade"
        value={values.grade}
        onChange={handleChange}
      />
      <select name="gradeSystem" value={values.gradeSystem} onChange={handleChange}>
        {GRADE_SYSTEM_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <select name="sourceApp" value={values.sourceApp} onChange={handleChange}>
        {SOURCE_APP_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <input
        name="externalId"
        placeholder="External ID"
        value={values.externalId}
        onChange={handleChange}
      />
      <input
        name="climbId"
        placeholder="Climb ID"
        value={values.climbId}
        onChange={handleChange}
      />
      <input
        name="sendDate"
        placeholder="Send date"
        type="date"
        value={values.sendDate}
        onChange={handleChange}
      />
      <input
        name="style"
        placeholder="Send style"
        value={values.style}
        onChange={handleChange}
      />
      <select name="ropeSendStyle" value={values.ropeSendStyle} onChange={handleChange}>
        {ROPE_SEND_STYLE_OPTIONS.map((option) => (
          <option key={option.value} value={option.value}>
            {option.label}
          </option>
        ))}
      </select>
      <input
        min="0"
        name="attempts"
        placeholder="Attempts"
        type="number"
        value={values.attempts}
        onChange={handleChange}
      />
      <input
        name="sourceUrl"
        placeholder="Source URL"
        value={values.sourceUrl}
        onChange={handleChange}
      />
      <textarea
        name="notes"
        placeholder="Notes"
        value={values.notes}
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

export default SendForm;
