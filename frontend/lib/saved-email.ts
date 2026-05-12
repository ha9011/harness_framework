const STORAGE_KEY = "saved_email";

export function getSavedEmail(): string | null {
  if (typeof window === "undefined") return null;
  return localStorage.getItem(STORAGE_KEY);
}

export function setSavedEmail(email: string): void {
  localStorage.setItem(STORAGE_KEY, email);
}

export function clearSavedEmail(): void {
  localStorage.removeItem(STORAGE_KEY);
}
