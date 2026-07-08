import { useCallback, useEffect, useState } from "react";
import TickCard from "../components/TickCard";
import TickForm from "../components/TickForm";
import {
  DEFAULT_TICK_VALUES,
  FILTER_OPTIONS,
  toTickPayload,
  type TickFormValues,
  type TickRecord,
} from "../tickConfig";
import "../App.css";

type TickPageProps = {
  onAuthExpired: () => void;
  token: string;
};

function TickPage({ onAuthExpired, token }: TickPageProps) {
  const [ticks, setTicks] = useState<TickRecord[]>([]);
  const [sourceFilter, setSourceFilter] = useState("ALL");
  const [gradeSystemFilter, setGradeSystemFilter] = useState("ALL");
  const [searchFilter, setSearchFilter] = useState("");
  const [message, setMessage] = useState("");

   const loadTicks = useCallback(async () => {
    try {
    const response = await fetch("/ticks", {
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

      setMessage(`Could not load ticks (${response.status}).`);
      return;
    }
    const data = await response.json();
    if (!Array.isArray(data)) {
      setMessage("Could not load ticks: unexpected response from the server.");
      return;                    // ← bail out before setTicks, so filter never sees a non-array
    }

    setTicks(data);
    setMessage("");
  } catch (error) {
    setMessage(`Could not load ticks: ${error}`);
  }
  }, [onAuthExpired, token]);


  useEffect(() => {
    const timeoutId = window.setTimeout(() => {
      loadTicks();
    }, 0);

    return () => window.clearTimeout(timeoutId);
  }, [loadTicks]);

  async function createTick(values: TickFormValues) {
    const response = await fetch("/ticks", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${token}`,
      },
      body: JSON.stringify(toTickPayload(values)),
    });

    if (!response.ok) {
      if (response.status === 401) {
        onAuthExpired();
        throw new Error("Your session expired. Please log in again.");
      }

      if (response.status === 403) {
        throw new Error("The backend rejected the create request (403).");
      }

      throw new Error(`Could not create tick (${response.status}).`);
    }

    await loadTicks();
  }

  const normalizedSearch = searchFilter.toLowerCase();
  const filteredTicks = ticks.filter((tick) => {
    const matchesSource =
      sourceFilter === "ALL" || tick.sourceApp === sourceFilter;
    const matchesGradeSystem =
      gradeSystemFilter === "ALL" || tick.gradeSystem === gradeSystemFilter;
    const matchesSearch =
      normalizedSearch === "" ||
      [
        tick.climbName,
        tick.location,
        tick.discipline,
        tick.tickType,
        tick.style,
        tick.ropeStyle,
        tick.grade,
        tick.notes,
        tick.externalId,
        tick.climbId,
      ].some((value) => value?.toLowerCase().includes(normalizedSearch));

    return matchesSource && matchesGradeSystem && matchesSearch;
  });

  return (
    <main className="tick-page">
      <TickForm
        className="create-tick-form"
        heading="Log Tick"
        initialValues={DEFAULT_TICK_VALUES}
        resetOnSubmit
        submitLabel="Log Tick"
        onSubmit={createTick}
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
      {filteredTicks.map((tick) => (
        <TickCard
          key={tick.id}
          tick={tick}
          onAuthExpired={onAuthExpired}
          onDelete={loadTicks}
          onUpdate={loadTicks}
          token={token}
        />
      ))}
    </main>
  );
}

export default TickPage;
