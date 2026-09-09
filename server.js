import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = parseInt(process.env.DEFAULT_APP_PORT || process.env.PORT || '3000', 10);
const HOST = '0.0.0.0';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

function getApkPath() {
  const p1 = path.join(__dirname, '.build-outputs', 'app-debug.apk');
  if (fs.existsSync(p1)) return p1;
  const p2 = path.join(__dirname, 'app', 'build', 'outputs', 'apk', 'debug', 'app-debug.apk');
  if (fs.existsSync(p2)) return p2;
  return null;
}

const htmlPath = path.join(__dirname, 'index.html');
let cachedHtml = '';
try {
  cachedHtml = fs.readFileSync(htmlPath, 'utf8');
} catch (e) {
  cachedHtml = '<!DOCTYPE html><html><head><title>AI Chatbot</title></head><body><h1>AI Chatbot Dev Server</h1></body></html>';
}

const server = http.createServer(async (req, res) => {
  const urlObj = new URL(req.url, `http://${req.headers.host || 'localhost:3000'}`);
  const pathname = urlObj.pathname;

  // CORS Headers
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');

  if (req.method === 'OPTIONS') {
    res.writeHead(204);
    res.end();
    return;
  }

  // Route: /api/health
  if (pathname === '/api/health') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'ok',
      uptime: process.uptime(),
      port: PORT,
      timestamp: new Date().toISOString()
    }));
    return;
  }

  // Route: /api/status
  if (pathname === '/api/status') {
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.end(JSON.stringify({
      status: 'active',
      apkAvailable: !!getApkPath(),
      geminiKeyConfigured: !!GEMINI_API_KEY,
      supportedModels: [
        'google/gemini-3.6-flash',
        'google/gemini-3.8-flash',
        'google/gemini-3.1-pro-preview',
        'meta-llama/llama-3.3-70b-instruct:free',
        'mistralai/mistral-7b-instruct:free',
        'deepseek/deepseek-chat'
      ]
    }));
    return;
  }

  // Route: /api/download/apk
  if (pathname === '/api/download/apk') {
    const apkPath = getApkPath();
    if (apkPath && fs.existsSync(apkPath)) {
      const stat = fs.statSync(apkPath);
      res.writeHead(200, {
        'Content-Type': 'application/vnd.android.package-archive',
        'Content-Length': stat.size,
        'Content-Disposition': 'attachment; filename="ai-chatbot-debug.apk"'
      });
      const stream = fs.createReadStream(apkPath);
      stream.pipe(res);
      return;
    } else {
      res.writeHead(404, { 'Content-Type': 'application/json' });
      res.end(JSON.stringify({ error: 'APK build artifact not found.' }));
      return;
    }
  }

  // Route: /api/chat
  if (pathname === '/api/chat' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const model = payload.model || 'google/gemini-3.6-flash';
        const messages = payload.messages || [];
        const customApiKey = payload.apiKey;

        // Helper function for free Gemini API proxy
        async function callGemini(targetModel, msgList) {
          if (!GEMINI_API_KEY) {
            throw new Error('Server GEMINI_API_KEY not found in environment.');
          }

          let geminiModel = 'gemini-3.6-flash';
          if (targetModel.includes('3.8')) {
            geminiModel = 'gemini-3.8-flash';
          } else if (targetModel.includes('pro')) {
            geminiModel = 'gemini-3.1-pro-preview';
          } else {
            geminiModel = 'gemini-3.6-flash';
          }

          const contents = [];
          let systemInstruction = null;

          for (const msg of msgList) {
            if (msg.role === 'system') {
              systemInstruction = { parts: [{ text: msg.content }] };
            } else if (msg.role === 'user') {
              contents.push({ role: 'user', parts: [{ text: msg.content }] });
            } else if (msg.role === 'assistant') {
              contents.push({ role: 'model', parts: [{ text: msg.content }] });
            }
          }

          const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${GEMINI_API_KEY}`;
          const reqBody = { contents };
          if (systemInstruction) reqBody.systemInstruction = systemInstruction;

          const geminiResp = await fetch(geminiUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(reqBody)
          });

          const geminiData = await geminiResp.json();
          if (!geminiResp.ok) {
            throw new Error(geminiData.error?.message || 'Gemini API call failed.');
          }

          const reply = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "I couldn't generate a response.";
          return { reply, model: geminiModel };
        }

        const isOpenRouterModel = model.startsWith('meta-llama/') || model.startsWith('mistralai/') || model.startsWith('deepseek/');

        // If user explicitly picked an external model (Meta/Mistral/DeepSeek)
        if (isOpenRouterModel) {
          const apiKey = customApiKey || process.env.OPENROUTER_API_KEY;
          if (!apiKey) {
            // Fallback immediately to free Gemini if no OpenRouter key
            const fallback = await callGemini('google/gemini-3.6-flash', messages);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              reply: `${fallback.reply}\n\n*(Note: No OpenRouter API key provided. Answered for free using Gemini 3.6 Flash.)*`,
              model: fallback.model,
              fallback: true
            }));
            return;
          }

          try {
            const openRouterResp = await fetch('https://openrouter.ai/api/v1/chat/completions', {
              method: 'POST',
              headers: {
                'Authorization': `Bearer ${apiKey}`,
                'Content-Type': 'application/json',
                'HTTP-Referer': 'https://ai.studio/build',
                'X-Title': 'AI Chatbot Companion'
              },
              body: JSON.stringify({
                model,
                messages,
                max_tokens: 2048 // Prevents OpenRouter from requiring 65536 credit reserve
              })
            });

            const orData = await openRouterResp.json();
            if (!openRouterResp.ok) {
              const errMsg = orData.error?.message || 'OpenRouter API request failed.';
              // If credit/quota/payment error, automatically fallback to free Gemini so user isn't stuck!
              if (openRouterResp.status === 402 || errMsg.toLowerCase().includes('credit') || errMsg.toLowerCase().includes('token')) {
                const fallback = await callGemini('google/gemini-3.6-flash', messages);
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({
                  reply: `${fallback.reply}\n\n*(Notice: OpenRouter credits depleted. Switched to free built-in Gemini 3.6 Flash at $0 cost.)*`,
                  model: fallback.model,
                  fallback: true
                }));
                return;
              }
              res.writeHead(openRouterResp.status, { 'Content-Type': 'application/json' });
              res.end(JSON.stringify({ error: errMsg }));
              return;
            }

            const replyText = orData.choices?.[0]?.message?.content || '';
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({ reply: replyText, model }));
            return;
          } catch (fetchErr) {
            // Network fallback to free Gemini
            const fallback = await callGemini('google/gemini-3.6-flash', messages);
            res.writeHead(200, { 'Content-Type': 'application/json' });
            res.end(JSON.stringify({
              reply: `${fallback.reply}\n\n*(Notice: OpenRouter connection failed. Switched to free built-in Gemini 3.6 Flash.)*`,
              model: fallback.model,
              fallback: true
            }));
            return;
          }
        }

        // Default & Google models: Always use 100% Free built-in Gemini Proxy
        const geminiResult = await callGemini(model, messages);
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ reply: geminiResult.reply, model: geminiResult.model }));

      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message || 'Internal server error' }));
      }
    });
    return;
  }

  // Root or any other path -> Serve index.html
  let html = cachedHtml;
  try {
    html = fs.readFileSync(htmlPath, 'utf8');
  } catch (e) {}

  res.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
  res.end(html);
});

server.listen(PORT, HOST, () => {
  console.log(`[Dev Server] Listening on http://${HOST}:${PORT}`);
});
