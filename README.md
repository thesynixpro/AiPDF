# AI PDF Assistant

An Android app that combines a full PDF toolkit with an AI assistant that turns
natural-language prompts into **real, downloadable PDF files**.

- **Package / namespace:** `com.aipdf.assistant`
- **Language:** Java 17, MVVM
- **Min / target SDK:** 26 / 34
- **PDF engine:** [PdfBox-Android](https://github.com/TomRoush/PdfBox-Android) (Apache-2.0) for generation & manipulation, and the platform `android.graphics.pdf.PdfRenderer` for fast on-screen viewing
- **AI providers:** Google Gemini (default) and OpenRouter, with dynamic model discovery

---

## Features

### AI document generation
- Describe a document in plain language ("a 5-page onboarding guide for new
  hires") and the assistant produces a structured document specification.
- The specification is rendered to a real PDF with a cover page, table of
  contents, headings, paragraphs, bullet/numbered lists, tables, page breaks,
  and "Page X of Y" footers.
- Clarifying questions are asked only when key details are genuinely missing.
- If a model returns prose instead of JSON, the app degrades gracefully instead
  of failing.

### PDF tools
Merge, split, extract pages, reorder, rotate, delete pages, watermark, protect
(password), unprotect, compress (lossless re-save), extract text, and build a
PDF from images. Every operation writes a **new** file and never mutates the
source.

### Reader & files
- Native paged reader for viewing, zooming and page navigation.
- Library of imported, generated and tool-produced documents with search, sort,
  rename, share, export and delete.

### Templates
14 ready-made document templates, each of which can be started manually or
filled in by the AI.

---

## Setup

### 1. Requirements
- Android SDK Platform 34 + Build-Tools 34.0.0
- JDK 17
- Gradle 8.7 (a wrapper is not committed; use a local `gradle` on PATH or add a wrapper)

`local.properties` must point at your SDK:

```properties
sdk.dir=/path/to/Android/sdk
```

### 2. Build

```bash
gradle assembleDebug        # -> app/build/outputs/apk/debug/app-debug.apk
gradle testDebugUnitTest     # runs the pure-JVM unit tests
```

### 3. Configure an AI provider

Open **Settings → AI settings**:

1. Pick a **provider tab** (Google Gemini or OpenRouter). Each provider keeps its
   own API key and model, so switching never overwrites the other.
2. Paste your API key and tap **Save**. The key is encrypted with the Android
   Keystore (AES/GCM) before being stored — it is never written in cleartext and
   never logged. Only a masked form (e.g. `sk-1…abcd`) is ever displayed.
3. Tap **Validate** to check the key, or **Fetch models** to download the list of
   generation-capable models. You can also type a model id manually.
4. Optionally enable **Set as default** to make this provider the active one.

Obtain keys from:
- Google Gemini — https://aistudio.google.com/app/apikey
- OpenRouter — https://openrouter.ai/keys

### 4. First run
The default PDF page size is **A4 / portrait**; change it under
**Settings → PDF defaults**. A provider must be configured before AI features
are enabled.

---

## Architecture

```
app/src/main/java/com/aipdf/assistant/
├─ AiPdfApp                     Application: init PdfBox, settings, database
├─ ai/                          Provider-agnostic AI layer
│  ├─ AiProvider                interface (chat, stream, listModels, validate)
│  ├─ ProviderManager           registry; keeps per-provider key + model
│  ├─ providers/                GeminiProvider, OpenRouterProvider (real HTTP)
│  ├─ models/                   DocumentSpec, DocumentSpecParser, ChatMessage, AiModel
│  └─ PromptLibrary             system instructions
├─ network/                     ApiClient (OkHttp), ModelListParsers (unit-tested)
├─ pdf/                         PdfGenerator, TextLayoutEngine, PdfOperations,
│                               PdfAnalyzer, PageRangeParser, PdfPageRenderer
├─ data/                        Room entities/DAOs, repositories
├─ security/                    CryptoManager + SecureKeyStore (Keystore AES/GCM)
├─ settings/                    AppSettings, ProviderIds
└─ ui/                          Activities, fragments, adapters (viewBinding)
```

The AI never emits a PDF directly. It emits a JSON `DocumentSpec`, which is
parsed (`DocumentSpecParser`), shown as an editable preview, and rendered by
`PdfGenerator`. This keeps the pipeline inspectable and provider-independent.

---

## Security & privacy

- API keys are encrypted at rest via `CryptoManager` (Android Keystore,
  AES/GCM) and accessed through `SecureKeyStore`.
- The OkHttp logging interceptor is set to `Level.NONE`; keys, prompts and
  response bodies are never logged.
- PDF parsing/editing and text extraction happen entirely on-device. Only the
  text you send to the assistant leaves the device, and only to the provider you
  configured.

---

## Testing

Pure-JVM unit tests cover the logic that does not require a device:

- `DocumentSpecParserTest` — JSON→spec parsing, code-fence stripping,
  clarification detection, round-trips.
- `ModelListParsersTest` — Gemini and OpenRouter model-list payload parsing.
- `PageRangeParserTest` — page-range strings (`"1-3, 5, 8-10"`).
- `TextLayoutEngineTest` — word wrapping, glyph transliteration, width fallback.

```bash
gradle testDebugUnitTest
```

---

## Compatibility note (aarch64 hosts)

Google ships Android build-tools as x86-64 binaries; they cannot execute on an
`aarch64` host. On such machines, provide a native aarch64 `aapt2` and point
Gradle at it in `gradle.properties`:

```properties
android.aapt2FromMavenOverride=/absolute/path/to/aapt2
```

The override **must be a file named `aapt2`**.

---

## Known limitations

- **Compress** performs a lossless structural rewrite (object renumbering and
  stream compression). It does not re-encode embedded images, so savings are
  modest. There is no "downsample images" mode.
- **No OCR.** Scanned/image-only PDFs yield little or no extractable text; the
  reader detects this but cannot read the content.
- Generated PDFs use the standard Type-1 fonts. Non-Latin scripts that fall
  outside WinAnsi are transliterated or shown as `?`; there is no CJK/Arabic
  font embedding yet.
- AI output quality and JSON-schema adherence depend on the selected model;
  weaker models may need a retry.
- API keys are required and are not bundled. No keys are included in this
  repository.
