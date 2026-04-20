export function extractJSON(text: string) {
    try {
        const match = text.match(/\[.*\]/s);
        if (!match) return null;
        return JSON.parse(match[0]);
    } catch {
        return null;
    }
}