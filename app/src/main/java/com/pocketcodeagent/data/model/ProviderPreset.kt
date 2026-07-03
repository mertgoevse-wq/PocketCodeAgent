package com.pocketcodeagent.data.model

data class ProviderPreset(
    val providerType: ProviderType,
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val modelOptions: List<String>,
    val supportsStreaming: Boolean = true
)

object ProviderPresets {
    val all: List<ProviderPreset> = listOf(
        ProviderPreset(
            providerType = ProviderType.OPENROUTER,
            displayName = "OpenRouter",
            baseUrl = "https://openrouter.ai/api/v1",
            defaultModel = "deepseek/deepseek-v4-flash",
            modelOptions = listOf(
                "deepseek/deepseek-v4-flash",
                "openrouter/auto",
                "openrouter/free",
                "google/gemini-3.5-flash",
                "anthropic/claude-sonnet-4.5",
                "openai/gpt-oss-120b",
                "qwen/qwen3-coder",
                "moonshotai/kimi-k2"
            )
        ),
        ProviderPreset(
            providerType = ProviderType.GEMINI_OPENAI,
            displayName = "Google Gemini",
            baseUrl = "https://generativelanguage.googleapis.com/v1beta/openai",
            defaultModel = "gemini-3.5-flash",
            modelOptions = listOf(
                "gemini-3.5-flash",
                "gemini-3.5-pro",
                "gemini-3.1-pro",
                "gemini-3.1-flash-lite",
                "gemini-3-flash",
                "gemini-2.5-pro",
                "gemini-2.5-flash",
                "gemini-2.5-flash-lite"
            )
        ),
        ProviderPreset(
            providerType = ProviderType.NVIDIA_NIM,
            displayName = "NVIDIA NIM",
            baseUrl = "https://integrate.api.nvidia.com/v1",
            defaultModel = "qwen/qwen3-coder-480b-a35b-instruct",
            modelOptions = listOf(
                "qwen/qwen3-coder-480b-a35b-instruct",
                "deepseek-ai/deepseek-v4-flash",
                "deepseek-ai/deepseek-v4-pro",
                "nvidia/nemotron-3-ultra-550b-a55b",
                "nvidia/nemotron-3-super-120b-a12b",
                "nvidia/nemotron-3-nano-30b-a3b",
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "moonshotai/kimi-k2-instruct",
                "moonshotai/kimi-k2-thinking",
                "meta/llama-3.3-70b-instruct"
            )
        ),
        ProviderPreset(
            providerType = ProviderType.GROQ,
            displayName = "Groq",
            baseUrl = "https://api.groq.com/openai/v1",
            defaultModel = "llama-3.3-70b-versatile",
            modelOptions = listOf(
                "llama-3.3-70b-versatile",
                "llama-3.1-8b-instant",
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "groq/compound",
                "groq/compound-mini",
                "meta-llama/llama-4-scout-17b-16e-instruct"
            )
        ),
        ProviderPreset(
            providerType = ProviderType.MISTRAL,
            displayName = "Mistral",
            baseUrl = "https://api.mistral.ai/v1",
            defaultModel = "mistral-medium-latest",
            modelOptions = listOf(
                "mistral-medium-latest",
                "mistral-large-latest",
                "mistral-small-latest",
                "codestral-latest",
                "devstral-small-latest",
                "devstral-2512",
                "magistral-medium-2509",
                "magistral-small-2509",
                "mistral-medium-2505",
                "mistral-small-2506"
            )
        ),
        ProviderPreset(
            providerType = ProviderType.TOGETHER,
            displayName = "Together",
            baseUrl = "https://api.together.xyz/v1",
            defaultModel = "moonshotai/Kimi-K2.7-Code",
            modelOptions = listOf(
                "moonshotai/Kimi-K2.7-Code",
                "deepseek-ai/DeepSeek-V4-Pro",
                "openai/gpt-oss-120b",
                "openai/gpt-oss-20b",
                "nvidia/nemotron-3-ultra-550b-a55b",
                "meta-llama/Llama-3.3-70B-Instruct-Turbo",
                "Qwen/Qwen3.6-Plus",
                "Qwen/Qwen3.5-9B",
                "MiniMaxAI/MiniMax-M3",
                "zai-org/GLM-5.2"
            )
        ),
        ProviderPreset(
            providerType = ProviderType.CUSTOM_OPENAI,
            displayName = "Custom",
            baseUrl = "",
            defaultModel = "",
            modelOptions = emptyList()
        )
    )

    fun forType(providerType: ProviderType): ProviderPreset {
        return all.first { it.providerType == providerType }
    }

    fun modelOptionsFor(providerType: ProviderType, selectedModel: String): List<String> {
        return (listOf(selectedModel).filter { it.isNotBlank() } + forType(providerType).modelOptions)
            .distinct()
    }
}
