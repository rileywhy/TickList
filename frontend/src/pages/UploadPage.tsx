import { useState, type ChangeEvent, type FormEvent } from "react";

type UploadPageProps = {
  onAuthExpired: () => void;
  token: string;
};

type ImportResponse = {
  filename: string | null;
  importedRows: number;
  skippedRows: number;
};

function UploadPage({ onAuthExpired, token }: UploadPageProps) {
  const [file, setFile] = useState<File | null>(null);
  const [message, setMessage] = useState("");
  const [isUploading, setIsUploading] = useState(false);

  function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
    setFile(event.target.files?.[0] ?? null);
    setMessage("");
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;

    if (file === null) {
      setMessage("Choose a CSV file first.");
      return;
    }

    const body = new FormData();
    body.append("file", file);

    setIsUploading(true);
    setMessage("");

    try {
      const response = await fetch("/imports/mountain-project", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
        body,
      });

      if (!response.ok) {
        if (response.status === 401) {
          onAuthExpired();
          setMessage("Your session expired. Please log in again.");
          return;
        }

        if (response.status === 403) {
          setMessage("The backend rejected this request (403).");
          return;
        }

        setMessage(`Could not import CSV (${response.status}).`);
        return;
      }

      const result = (await response.json()) as ImportResponse;
      setMessage(
        `Imported ${result.importedRows} sends from ${result.filename ?? file.name}. Skipped ${result.skippedRows}.`
      );
      setFile(null);
      form.reset();
    } catch {
      setMessage("Could not reach the backend.");
    } finally {
      setIsUploading(false);
    }
  }

  return (
    <main className="upload-page">
      <form className="upload-form" onSubmit={handleSubmit}>
        <h1>Import CSV</h1>
        <label className="upload-file-field">
          <span>Mountain Project CSV</span>
          <input accept=".csv,text/csv" name="file" type="file" onChange={handleFileChange} />
        </label>
        <button type="submit" disabled={isUploading}>
          {isUploading ? "Importing..." : "Import CSV"}
        </button>
        {file && <p className="upload-file-name">{file.name}</p>}
        {message && <p role="alert">{message}</p>}
      </form>
    </main>
  );
}

export default UploadPage;
