#include <jni.h>
#include <string>
#include <vector>
#include <algorithm>
#include "llama.h"

extern "C" JNIEXPORT jstring JNICALL
Java_com_ninoh_aiagent_MainActivity_nativeGenerate(JNIEnv* env, jobject,
        jstring jModelPath, jstring jPrompt, jint maxTokens) {
    const char* modelPath = env->GetStringUTFChars(jModelPath, nullptr);
    const char* promptText = env->GetStringUTFChars(jPrompt, nullptr);
    std::string output;
    llama_backend_init();

    llama_model_params modelParams = llama_model_default_params();
    modelParams.n_gpu_layers = 0;
    llama_model* model = llama_model_load_from_file(modelPath, modelParams);
    if (!model) {
        output = "Tsy afaka nisokatra ilay modely. Hamarino fa GGUF mifanaraka izy ary ampy ny RAM.";
    } else {
        llama_context_params ctxParams = llama_context_default_params();
        ctxParams.n_ctx = 2048;
        ctxParams.n_batch = 512;
        llama_context* ctx = llama_init_from_model(model, ctxParams);
        if (!ctx) {
            output = "Tsy ampy ny loharanon'ny finday hanombohana ity modely ity.";
        } else {
            const llama_vocab* vocab = llama_model_get_vocab(model);
            std::string prompt(promptText);
            std::string wrapped = "[INST] " + prompt + " [/INST]";
            int needed = -llama_tokenize(vocab, wrapped.c_str(), (int)wrapped.size(), nullptr, 0, true, true);
            std::vector<llama_token> tokens(needed + 8);
            int count = llama_tokenize(vocab, wrapped.c_str(), (int)wrapped.size(), tokens.data(), (int)tokens.size(), true, true);
            if (count < 0) {
                output = "Tsy voavaky ilay hafatra.";
            } else {
                tokens.resize(count);
                llama_batch batch = llama_batch_get_one(tokens.data(), (int)tokens.size());
                if (llama_decode(ctx, batch) != 0) {
                    output = "Tsy nahavita namaky ny prompt ilay modely.";
                } else {
                    llama_sampler* sampler = llama_sampler_chain_init(llama_sampler_chain_default_params());
                    llama_sampler_chain_add(sampler, llama_sampler_init_top_p(0.9f, 1));
                    llama_sampler_chain_add(sampler, llama_sampler_init_temp(0.7f));
                    llama_sampler_chain_add(sampler, llama_sampler_init_dist(LLAMA_DEFAULT_SEED));
                    int limit = std::max(1, std::min((int)maxTokens, 512));
                    for (int i = 0; i < limit; ++i) {
                        llama_token token = llama_sampler_sample(sampler, ctx, -1);
                        if (llama_vocab_is_eog(vocab, token)) break;
                        char piece[1024];
                        int n = llama_token_to_piece(vocab, token, piece, sizeof(piece), 0, true);
                        if (n > 0) output.append(piece, n);
                        llama_token next = token;
                        llama_batch nextBatch = llama_batch_get_one(&next, 1);
                        if (llama_decode(ctx, nextBatch) != 0) break;
                    }
                    llama_sampler_free(sampler);
                }
            }
            llama_free(ctx);
        }
        llama_model_free(model);
    }
    llama_backend_free();
    env->ReleaseStringUTFChars(jModelPath, modelPath);
    env->ReleaseStringUTFChars(jPrompt, promptText);
    return env->NewStringUTF(output.c_str());
}
