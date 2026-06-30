import { useCallback, useEffect, useState } from "react";
import SendCard from "../components/SendCard";
import SendForm from "../components/SendForm";
import {
  DEFAULT_SEND_VALUES,
  FILTER_OPTIONS,
  toSendPayload,
  type SendFormValues,
  type SendRecord,
} from "../sendConfig";
import "../App.css";

type SendPageProps = {
  onAuthExpired: () => void;
  token: string;
};

function SendPage({ onAuthExpired, token }: SendPageProps) {
  const [sends, setSends] = useState<SendRecord[]>([]);
  const [sourceFilter, setSourceFilter] = useState("ALL");
  const [gradeSystemFilter, setGradeSystemFilter] = useState("ALL");
  const [searchFilter, setSearchFilter] = useState("");
  const [message, setMessage] = useState("");

  const loadSends = useCallback(async () => {
    const response = await fetch("/sends", {
      headers: {
        Authorization: `Bearer ${token}`,
      },
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

      setMessage(`Could not load sends (${response.status}).`);
      return;
    }

    const data = await response.json();
    setSends(data);
    setMessage("");
  }, [onAuthExpired, token]);

  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      loadSends();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [loadSends]);

  async function createSend(values: SendFormValues) {
    const response = await fetch("/sends", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(toSendPayload(values)),
    });

    if (!response.ok) {
      if (response.status === 401) {
        onAuthExpired();
        throw new Error("Your session expired. Please log in again.");
      }

      if (response.status === 403) {
        throw new Error("The backend rejected the create request (403).");
      }

      throw new Error(`Could not create send (${response.status}).`);
    }

    await loadSends();
  }

  const normalizedSearch = searchFilter.toLowerCase();
  const filteredSends = sends.filter((send) => {
    const matchesSource =
      sourceFilter === "ALL" || send.sourceApp === sourceFilter;
    const matchesGradeSystem =
      gradeSystemFilter === "ALL" || send.gradeSystem === gradeSystemFilter;
    const matchesSearch =
      normalizedSearch === "" ||
      [
        send.climbName,
        send.location,
        send.discipline,
        send.style,
        send.ropeSendStyle,
        send.grade,
        send.notes,
        send.externalId,
        send.climbId,
      ].some((value) => value?.toLowerCase().includes(normalizedSearch));

    return matchesSource && matchesGradeSystem && matchesSearch;
  });

  return (
    <main className="send-page">
      <SendForm
        className="create-send-form"
        heading="Log Send"
        initialValues={DEFAULT_SEND_VALUES}
        resetOnSubmit
        submitLabel="Log Send"
        onSubmit={createSend}
      />
      <div className="filters">
        <input
          type="text"
          placeholder="Search"
          value={searchFilter}
          onChange={(event) => setSearchFilter(event.target.value)}
        />
        <select
          value={sourceFilter}
          onChange={(event) => setSourceFilter(event.target.value)}
        >
          {FILTER_OPTIONS.sourceApp.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
        <select
          value={gradeSystemFilter}
          onChange={(event) => setGradeSystemFilter(event.target.value)}
        >
          {FILTER_OPTIONS.gradeSystem.map((option) => (
            <option key={option.value} value={option.value}>
              {option.label}
            </option>
          ))}
        </select>
      </div>
      {message && <p role="alert">{message}</p>}
      {filteredSends.map((send) => (
        <SendCard
          key={send.id}
          send={send}
          onAuthExpired={onAuthExpired}
          onDelete={loadSends}
          onUpdate={loadSends}
          token={token}
        />
      ))}
    </main>
  );
}

export default SendPage;
