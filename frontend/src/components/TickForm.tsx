import { useState, type ChangeEvent, type FormEvent } from "react";
import {
  DISCIPLINE_OPTIONS,
  GRADE_SYSTEM_OPTIONS,
  ROPE_STYLE_OPTIONS,
  SOURCE_APP_OPTIONS,
  TICK_TYPE_OPTIONS,
  type TickFormValues,
} from "../tickConfig";

type TickFormProps = {
  className?: string;
  heading?: string;
  initialValues: TickFormValues;
  resetOnSubmit?: boolean;
  submitLabel: string;
  onSubmit: (values: TickFormValues) => Promise<void> | void;
  onCancel?: () => void;
};

function TickForm({
  className,
  heading,
  initialValues,
  resetOnSubmit = false,
  submitLabel,
  onSubmit,
  onCancel,
}: TickFormProps) {
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
          : "Could not save tick changes."
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
      <select name="tickType" value={values.tickType} onChange={handleChange}>
        {TICK_TYPE_OPTIONS.map((option) => (
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
        name="tickDate"
        placeholder="Tick date"
        type="date"
        value={values.tickDate}
        onChange={handleChange}
      />
      <input
        name="style"
        placeholder="Tick style"
        value={values.style}
        onChange={handleChange}
      />
      <select name="ropeStyle" value={values.ropeStyle} onChange={handleChange}>
        {ROPE_STYLE_OPTIONS.map((option) => (
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

export default TickForm;
