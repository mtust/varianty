const BASE64_ALPHABET = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/';

/** Decodes a base64url string to text (ASCII/Latin-1 is enough for the numeric `exp` claim). */
function decodeBase64Url(input: string): string {
  const base64 = input.replace(/-/g, '+').replace(/_/g, '/');
  let output = '';
  let buffer = 0;
  let bits = 0;
  for (const char of base64) {
    if (char === '=') break;
    const value = BASE64_ALPHABET.indexOf(char);
    if (value < 0) throw new Error('Invalid base64');
    buffer = (buffer << 6) | value;
    bits += 6;
    if (bits >= 8) {
      bits -= 8;
      output += String.fromCharCode((buffer >> bits) & 0xff);
    }
  }
  return output;
}

/** Reads a numeric claim (seconds since epoch for `exp`/`iat`), or null if it can't be read. */
function getNumericClaim(token: string, claim: 'exp' | 'iat'): number | null {
  try {
    const payload = token.split('.')[1];
    if (!payload) return null;
    const match = decodeBase64Url(payload).match(new RegExp(`"${claim}"\\s*:\\s*(\\d+)`));
    return match ? Number(match[1]) : null;
  } catch {
    return null;
  }
}

/** Returns the token's `exp` (seconds since epoch), or null if it can't be read. */
export function getTokenExpiry(token: string): number | null {
  return getNumericClaim(token, 'exp');
}

/** Returns the token's `iat` (seconds since epoch), or null if it can't be read. */
export function getTokenIssuedAt(token: string): number | null {
  return getNumericClaim(token, 'iat');
}

/**
 * True if the token is past its expiry (with a small margin for clock skew and request time).
 * A token whose expiry can't be read is treated as still valid; the server has the final say.
 */
export function isTokenExpired(token: string, marginSeconds = 30): boolean {
  const exp = getTokenExpiry(token);
  if (exp === null) return false;
  return Date.now() / 1000 >= exp - marginSeconds;
}

/** Fallback lifetime of a registered token when it has no `iat` (matches the backend's 30 days). */
const DEFAULT_LIFETIME_SECONDS = 30 * 24 * 60 * 60;

/** True once less than half of the token's lifetime is left. */
export function isPastHalfLife(token: string): boolean {
  const exp = getTokenExpiry(token);
  if (exp === null) return false;
  const iat = getTokenIssuedAt(token);
  const lifetime = iat !== null && exp > iat ? exp - iat : DEFAULT_LIFETIME_SECONDS;
  return exp - Date.now() / 1000 < lifetime / 2;
}
