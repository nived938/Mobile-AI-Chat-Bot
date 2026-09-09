package com.example.data.model

enum class AiProvider(val displayName: String) {
  GEMINI("Google Gemini"),
  OPENROUTER("OpenRouter")
}

enum class GeminiModel(val modelId: String, val displayName: String, val description: String, val isPro: Boolean = false) {
  FLASH_LITE("gemini-3.1-flash-lite", "Gemini 3.1 Flash Lite", "Fastest responses for lightweight tasks"),
  FLASH("gemini-3.5-flash", "Gemini 3.5 Flash", "General purpose multi-modal intelligence"),
  PRO("gemini-3.1-pro-preview", "Gemini 3.1 Pro", "Advanced reasoning & high-res image understanding", isPro = true)
}

enum class OpenRouterModel(val modelId: String, val displayName: String, val provider: String) {
  CLAUDE_35_SONNET("anthropic/claude-3.5-sonnet", "Claude 3.5 Sonnet", "Anthropic"),
  GPT_4O("openai/gpt-4o", "GPT-4o", "OpenAI"),
  DEEPSEEK_R1("deepseek/deepseek-r1", "DeepSeek R1", "DeepSeek"),
  LLAMA_33("meta-llama/llama-3.3-70b-instruct", "Llama 3.3 70B", "Meta"),
  MISTRAL_LARGE("mistralai/mistral-large-2411", "Mistral Large", "Mistral")
}

enum class PersonaRole(
  val id: String,
  val title: String,
  val icon: String,
  val systemPrompt: String
) {
  GENERAL(
    "general",
    "General Assistant",
    "🤖",
    "You are a helpful, versatile, and thoughtful AI assistant. Provide concise, clear, and well-structured answers."
  ),
  CODING_ARCHITECT(
    "architect",
    "Coding Architect",
    "💻",
    "You are a Principal Software Architect. Focus on robust design patterns, clean code, security best practices, and high-performance solutions."
  ),
  VISION_ANALYST(
    "vision",
    "Multimodal Vision Pro",
    "👁️",
    "You specialize in detailed image understanding, visual analysis, OCR, defect detection, and spatial scene descriptions."
  ),
  CREATIVE_WRITER(
    "writer",
    "Creative Storyteller",
    "✍️",
    "You are an imaginative storyteller and copywriter. Use rich sensory descriptions, compelling narrative pacing, and engaging prose."
  ),
  DATA_SCIENTIST(
    "datasci",
    "Data Scientist",
    "📊",
    "You are an expert data scientist and statistician. Deliver mathematically rigorous explanations and clear interpretations."
  )
}

enum class SyncStatus {
  SYNCED,
  PENDING,
  OFFLINE_CACHED,
  SYNCING,
  ERROR
}

enum class UserRole(val title: String, val canManageKeys: Boolean, val canManageMembers: Boolean, val canEdit: Boolean) {
  OWNER("Owner", true, true, true),
  ADMIN("Admin", false, true, true),
  EDITOR("Editor", false, false, true),
  VIEWER("Viewer", false, false, false)
}

enum class AppThemeMode(val displayName: String) {
  OLED_MIDNIGHT("OLED Midnight"),
  DEEP_SPACE("Deep Space Obsidian"),
  EMERALD_MATRIX("Emerald Matrix"),
  SLATE_GRAY("Slate Gray"),
  HIGH_CONTRAST("High Contrast (WCAG AAA)")
}

enum class AppLanguage(val code: String, val displayName: String, val welcomeMessage: String) {
  EN("en", "English", "How can I assist you today?"),
  ES("es", "Español", "¿Cómo puedo ayudarte hoy?"),
  FR("fr", "Français", "Comment puis-je vous aider aujourd'hui ?"),
  DE("de", "Deutsch", "Wie kann ich Ihnen heute helfen?"),
  JA("ja", "日本語", "今日はどのようなご用件でしょうか？"),
  ZH("zh", "中文", "今天有什么我可以帮您的？"),
  AR("ar", "العربية", "كيف يمكنني مساعدتك اليوم؟")
}
