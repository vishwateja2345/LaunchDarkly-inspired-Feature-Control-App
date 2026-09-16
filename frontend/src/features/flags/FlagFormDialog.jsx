import { useId, useState } from "react";
import { createPortal } from "react-dom";
import { Modal } from "../../shared/components/Modal.jsx";
import { MaterialIcon } from "../../shared/components/MaterialIcon.jsx";
import { fieldErrorsFrom, firstErrorForPrefix } from "../../shared/fieldErrors.js";
import { flagsApi } from "./flags.api.js";

function emptyVariation() {
    return { value: "", name: "", description: "" };
}

export function FlagFormDialog({ onClose, onCreated, projectId }) {
    const titleId = `flag-form-${useId().replace(/:/g, "")}`;
    const [name, setName] = useState("");
    const [key, setKey] = useState("");
    const [description, setDescription] = useState("");
    const [flagType, setFlagType] = useState("boolean");
    const [temporary, setTemporary] = useState(true);
    const [tags, setTags] = useState("");
    const [variations, setVariations] = useState([emptyVariation(), emptyVariation()]);
    const [errors, setErrors] = useState({});
    const [formError, setFormError] = useState("");
    const [busy, setBusy] = useState(false);

    const updateVariation = (index, field, value) => {
        setVariations((current) =>
            current.map((variation, candidateIndex) =>
                candidateIndex === index ? { ...variation, [field]: value } : variation,
            ),
        );
    };

    const submit = async (event) => {
        event.preventDefault();
        setBusy(true);
        setErrors({});
        setFormError("");
        try {
            const input = {
                name: name.trim(),
                key: key.trim() || undefined,
                description: description.trim(),
                flagType,
                temporary,
                tags: tags
                    .split(",")
                    .map((tag) => tag.trim())
                    .filter(Boolean),
            };
            if (flagType === "multivariate") {
                input.variations = variations
                    .filter((variation) => variation.value.trim() || variation.name.trim())
                    .map((variation) => ({
                        value: variation.value.trim(),
                        name: variation.name.trim() || variation.value.trim(),
                        description: variation.description.trim(),
                    }));
            }
            await flagsApi.create(projectId, input);
            onCreated();
        } catch (caught) {
            setFormError(caught.message || "Unable to create this flag.");
            setErrors(fieldErrorsFrom(caught));
        } finally {
            setBusy(false);
        }
    };

    return createPortal(
        <Modal className="form-dialog-modal" labelledBy={titleId} onClose={() => !busy && onClose()}>
            <form className="form-dialog" onSubmit={submit}>
                <h2 id={titleId}>New feature flag</h2>

                <div className="field-group">
                    <label htmlFor="flag-name">Name</label>
                    <input
                        data-autofocus
                        id="flag-name"
                        required
                        value={name}
                        onChange={(event) => setName(event.target.value)}
                    />
                    {errors.name && <span className="field-error">{errors.name[0]}</span>}
                </div>

                <div className="field-group">
                    <label htmlFor="flag-key">Key</label>
                    <input
                        id="flag-key"
                        placeholder="auto-generated from name if left blank"
                        value={key}
                        onChange={(event) => setKey(event.target.value)}
                    />
                    {errors.key && <span className="field-error">{errors.key[0]}</span>}
                </div>

                <div className="field-group">
                    <label htmlFor="flag-description">Description</label>
                    <textarea
                        id="flag-description"
                        value={description}
                        onChange={(event) => setDescription(event.target.value)}
                    />
                </div>

                <div className="field-row">
                    <div className="field-group">
                        <label htmlFor="flag-type">Type</label>
                        <select id="flag-type" value={flagType} onChange={(event) => setFlagType(event.target.value)}>
                            <option value="boolean">Boolean (on/off)</option>
                            <option value="multivariate">Multivariate</option>
                        </select>
                    </div>
                    <div className="field-group">
                        <label htmlFor="flag-tags">Tags</label>
                        <input
                            id="flag-tags"
                            placeholder="comma, separated"
                            value={tags}
                            onChange={(event) => setTags(event.target.value)}
                        />
                    </div>
                </div>

                <label className="checkbox-row">
                    <input type="checkbox" checked={temporary} onChange={(event) => setTemporary(event.target.checked)} />
                    Temporary flag (a rollout/experiment flag intended to be retired later)
                </label>

                {flagType === "multivariate" && (
                    <div className="field-group" style={{ marginTop: 12 }}>
                        <label>Variations</label>
                        {variations.map((variation, index) => (
                            <div className="field-row" key={index} style={{ marginBottom: 6 }}>
                                <input
                                    aria-label={`Variation ${index + 1} value`}
                                    placeholder="value"
                                    value={variation.value}
                                    onChange={(event) => updateVariation(index, "value", event.target.value)}
                                />
                                <input
                                    aria-label={`Variation ${index + 1} name`}
                                    placeholder="display name"
                                    value={variation.name}
                                    onChange={(event) => updateVariation(index, "name", event.target.value)}
                                />
                                {variations.length > 2 && (
                                    <button
                                        aria-label="Remove variation"
                                        className="icon-button"
                                        type="button"
                                        onClick={() =>
                                            setVariations((current) => current.filter((_, i) => i !== index))
                                        }
                                    >
                                        <MaterialIcon size={16}>close</MaterialIcon>
                                    </button>
                                )}
                            </div>
                        ))}
                        <button
                            className="link-button"
                            type="button"
                            onClick={() => setVariations((current) => [...current, emptyVariation()])}
                        >
                            + Add variation
                        </button>
                        {firstErrorForPrefix(errors, "variations") && (
                            <span className="field-error">{firstErrorForPrefix(errors, "variations")}</span>
                        )}
                    </div>
                )}

                {formError && (
                    <p className="inline-error" role="alert">
                        {formError}
                    </p>
                )}

                <footer>
                    <button className="secondary-button" disabled={busy} type="button" onClick={onClose}>
                        Cancel
                    </button>
                    <button className="primary-button" disabled={busy} type="submit">
                        {busy ? "Creating…" : "Create flag"}
                    </button>
                </footer>
            </form>
        </Modal>,
        document.body,
    );
}
