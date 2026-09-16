/**
 * Backend validators report array-item errors with indexed keys like
 * "variations[1].value" or "rules[0].attribute" (see FieldErrors.java), not the
 * plain "variations"/"rules" section name. Components that want to show a single
 * section-level error message need to search by prefix instead of doing a direct
 * key lookup, or the message is silently dropped and only the generic top-level
 * "Request validation failed." ever reaches the user.
 */

export function fieldErrorsFrom(error) {
    return error?.details?.fieldErrors && typeof error.details.fieldErrors === "object"
        ? error.details.fieldErrors
        : {};
}

/** All messages for a section, matching the exact key or any "prefix[...]"/"prefix...." indexed key. */
export function errorsForPrefix(fieldErrors, prefix) {
    if (!fieldErrors) return [];

    return Object.entries(fieldErrors)
        .filter(([key]) => key === prefix || key.startsWith(`${prefix}[`) || key.startsWith(`${prefix}.`))
        .flatMap(([, messages]) => messages);
}

/** Convenience for showing one line under a section (e.g. the Variations list, a Rules list). */
export function firstErrorForPrefix(fieldErrors, prefix) {
    return errorsForPrefix(fieldErrors, prefix)[0];
}
