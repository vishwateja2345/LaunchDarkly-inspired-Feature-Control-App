import { SelectMenu } from "../../shared/components/SelectMenu.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { OutcomeEditor } from "./OutcomeEditor.jsx";

const OPERATORS = [
    { value: "in", label: "is any of" },
    { value: "notIn", label: "is none of" },
    { value: "equals", label: "is" },
    { value: "notEquals", label: "is not" },
    { value: "contains", label: "contains" },
    { value: "greaterThan", label: "is greater than" },
    { value: "lessThan", label: "is less than" },
    { value: "segmentMatch", label: "is in segment" },
];

function valuesToText(values) {
    return (values || []).join(", ");
}

function textToValues(text) {
    return text
        .split(",")
        .map((value) => value.trim())
        .filter(Boolean);
}

export function RuleEditor({ index, onChange, onRemove, rule, variations }) {
    const updateClause = (clauseIndex, patch) => {
        onChange({
            ...rule,
            clauses: rule.clauses.map((clause, candidateIndex) =>
                candidateIndex === clauseIndex ? { ...clause, ...patch } : clause,
            ),
        });
    };

    const addClause = () => {
        onChange({ ...rule, clauses: [...rule.clauses, { attribute: "", operator: "in", values: [] }] });
    };

    const removeClause = (clauseIndex) => {
        onChange({ ...rule, clauses: rule.clauses.filter((_, candidateIndex) => candidateIndex !== clauseIndex) });
    };

    return (
        <div className="rule-card">
            <div className="rule-card-header">
                <MaterialIcon size={18}>rule</MaterialIcon>
                <input
                    aria-label={`Rule ${index + 1} description`}
                    placeholder={`Rule ${index + 1}`}
                    value={rule.description}
                    onChange={(event) => onChange({ ...rule, description: event.target.value })}
                />
                <button aria-label="Remove rule" className="icon-button" type="button" onClick={onRemove}>
                    <MaterialIcon size={18}>delete</MaterialIcon>
                </button>
            </div>

            {rule.clauses.map((clause, clauseIndex) => (
                <div className="clause-row" key={clauseIndex}>
                    {clauseIndex > 0 && <span className="tag-chip">AND</span>}
                    {clause.operator !== "segmentMatch" && (
                        <input
                            aria-label="Attribute"
                            placeholder="attribute (e.g. plan, country, betaOptIn)"
                            value={clause.attribute || ""}
                            onChange={(event) => updateClause(clauseIndex, { attribute: event.target.value })}
                        />
                    )}
                    <SelectMenu
                        ariaLabel="Operator"
                        value={clause.operator}
                        onChange={(operator) => updateClause(clauseIndex, { operator })}
                        options={OPERATORS}
                    />
                    <input
                        aria-label={clause.operator === "segmentMatch" ? "Segment keys" : "Values"}
                        placeholder={clause.operator === "segmentMatch" ? "segment-key-one, segment-key-two" : "value one, value two"}
                        value={valuesToText(clause.values)}
                        onChange={(event) => updateClause(clauseIndex, { values: textToValues(event.target.value) })}
                    />
                    {rule.clauses.length > 1 && (
                        <button
                            aria-label="Remove condition"
                            className="icon-button"
                            type="button"
                            onClick={() => removeClause(clauseIndex)}
                        >
                            <MaterialIcon size={16}>close</MaterialIcon>
                        </button>
                    )}
                </div>
            ))}
            <button className="link-button" type="button" onClick={addClause}>
                + Add condition
            </button>

            <OutcomeEditor
                outcome={{ variationId: rule.variationId, rollout: rule.rollout }}
                variations={variations}
                onChange={(outcome) => onChange({ ...rule, ...outcome })}
            />
        </div>
    );
}
