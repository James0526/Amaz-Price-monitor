import base64
import html
import json
import re
import urllib.request
from urllib.parse import urlparse


USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"
)

TITLE_PATTERNS = [
    r'id="productTitle"[^>]*>(.*?)<',
    r'class="product-title-word-break"[^>]*>(.*?)<',
]

PRICE_PATTERNS = [
    r'id="priceblock_ourprice"[^>]*>(.*?)<',
    r'id="priceblock_dealprice"[^>]*>(.*?)<',
    r'id="priceblock_saleprice"[^>]*>(.*?)<',
    r'class="a-price-whole"[^>]*>(.*?)<',
    r'class="a-offscreen"[^>]*>(.*?)<',
]


def lambda_handler(event, context):
    url = _get_url_from_event(event)
    if not url:
        return _response(400, {"error": "Missing required 'url' parameter."})

    normalized_url = _normalize_url(url)
    if not normalized_url:
        return _response(400, {"error": "Invalid URL."})

    if not _is_allowed_amazon_url(normalized_url):
        return _response(400, {"error": "URL must be an Amazon product page."})

    try:
        page_html = _fetch_page(normalized_url)
        if _looks_like_captcha(page_html):
            return _response(
                502,
                {
                    "error": "Amazon blocked the request.",
                    "message": "Received a robot check or captcha page.",
                },
            )

        title = _extract_title(page_html)
        price_text = _extract_price(page_html)
        currency, amount = _parse_price_amount(price_text) if price_text else (None, None)

        return _response(
            200,
            {
                "url": normalized_url,
                "title": title,
                "price": price_text,
                "price_amount": amount,
                "currency": currency,
            },
        )
    except Exception as exc:  # noqa: BLE001 - Keep response stable for API callers.
        return _response(
            500,
            {"error": "Failed to fetch product data.", "message": str(exc)},
        )


def _get_url_from_event(event):
    if not isinstance(event, dict):
        return None

    direct_url = event.get("url")
    if isinstance(direct_url, str):
        return direct_url

    query_params = event.get("queryStringParameters") or {}
    if isinstance(query_params, dict):
        qs_url = query_params.get("url")
        if isinstance(qs_url, str):
            return qs_url

    body = event.get("body")
    if not body:
        return None

    if event.get("isBase64Encoded"):
        body = base64.b64decode(body).decode("utf-8", "replace")

    if isinstance(body, (bytes, bytearray)):
        body = body.decode("utf-8", "replace")

    if isinstance(body, str):
        try:
            payload = json.loads(body)
        except json.JSONDecodeError:
            return None
        if isinstance(payload, dict):
            url = payload.get("url")
            if isinstance(url, str):
                return url

    return None


def _normalize_url(url):
    if not isinstance(url, str):
        return None

    cleaned = url.strip()
    if not cleaned:
        return None

    if not cleaned.startswith(("http://", "https://")):
        cleaned = f"https://{cleaned}"

    parsed = urlparse(cleaned)
    if not parsed.netloc:
        return None

    normalized = parsed._replace(fragment="").geturl()
    return normalized


def _is_allowed_amazon_url(url):
    parsed = urlparse(url)
    host = parsed.netloc.split(":")[0].lower()
    if not host:
        return False

    parts = [part for part in host.split(".") if part]
    for index, part in enumerate(parts):
        if part == "amazon":
            suffix_parts = len(parts) - index - 1
            if suffix_parts in (1, 2):
                return True
    return False


def _fetch_page(url):
    request = urllib.request.Request(
        url,
        headers={
            "User-Agent": USER_AGENT,
            "Accept-Language": "en-US,en;q=0.9",
            "Accept": "text/html,application/xhtml+xml",
        },
    )
    with urllib.request.urlopen(request, timeout=10) as response:
        content_type = response.headers.get("Content-Type", "")
        charset_match = re.search(r"charset=([^\s;]+)", content_type, re.IGNORECASE)
        charset = charset_match.group(1) if charset_match else "utf-8"
        return response.read().decode(charset, "replace")


def _extract_title(page_html):
    return _extract_with_patterns(page_html, TITLE_PATTERNS)


def _extract_price(page_html):
    return _extract_with_patterns(page_html, PRICE_PATTERNS)


def _extract_with_patterns(page_html, patterns):
    for pattern in patterns:
        match = re.search(pattern, page_html, re.IGNORECASE | re.DOTALL)
        if match:
            text = html.unescape(match.group(1))
            cleaned = _clean_text(text)
            if cleaned:
                return cleaned
    return None


def _parse_price_amount(price_text):
    if not price_text:
        return None, None

    currency_match = re.search(r"[\$€£¥₹]", price_text)
    currency = currency_match.group(0) if currency_match else None

    number_match = re.search(r"(\d[\d,.]*)", price_text)
    if not number_match:
        return currency, None

    number = number_match.group(1)
    normalized = _normalize_number(number)
    try:
        return currency, float(normalized)
    except ValueError:
        return currency, None


def _normalize_number(number):
    if "," in number and "." in number:
        return number.replace(",", "")
    if "," in number and "." not in number:
        return number.replace(",", ".")
    return number


def _clean_text(text):
    cleaned = re.sub(r"\s+", " ", text)
    return cleaned.strip()


def _looks_like_captcha(page_html):
    lowered = page_html.lower()
    return "captcha" in lowered or "robot check" in lowered


def _response(status_code, payload):
    return {
        "statusCode": status_code,
        "headers": {"Content-Type": "application/json"},
        "body": json.dumps(payload),
    }
