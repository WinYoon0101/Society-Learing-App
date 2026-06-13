export function extractJSON(text: string) {
    try {
        const start = text.indexOf("[");
        const end = text.lastIndexOf("]");

        if (start === -1 || end === -1) return null;

        const jsonString = text.substring(start, end + 1);

        return JSON.parse(jsonString);
    } catch {
        return null;
    }
}