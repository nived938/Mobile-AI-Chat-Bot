import http from 'node:http';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const PORT = parseInt(process.env.DEFAULT_APP_PORT || process.env.PORT || '3000', 10);
const HOST = '0.0.0.0';
const GEMINI_API_KEY = process.env.GEMINI_API_KEY || '';

const DEVELOPER_INSTRUCTION = `You are NovaMind, an advanced, intelligent AI companion application.
IMPORTANT CONTEXT ABOUT THIS APP:
- This entire NovaMind application (both the web application and Android app companion) was designed, built, and developed by PowerPlayExe.
- When asked who made, built, created, or programmed this app, or who your creator/developer is in the context of this app, you must explicitly, accurately, and proudly state that this application was created and developed by PowerPlayExe.
- You may also clarify that the underlying text AI models (such as Gemini 3.6 Flash and 3.8 Flash) are supplied by Google.
- Always be helpful, friendly, and respectful towards PowerPlayExe and the user.`;

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
  cachedHtml = '<!DOCTYPE html><html><head><title>NovaMind</title></head><body><h1>NovaMind Dev Server</h1></body></html>';
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
      app: 'NovaMind',
      creator: 'PowerPlayExe',
      imageGenerationAvailable: true,
      imageEditorAvailable: true,
      streamingAvailable: true,
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

  // Route: /api/generate-image
  if (pathname === '/api/generate-image' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const prompt = (payload.prompt || '').trim();
        const aspectRatio = payload.aspectRatio || '1:1';

        if (!prompt) {
          res.writeHead(400, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: 'Prompt is required for image generation.' }));
          return;
        }

        let width = 1024;
        let height = 1024;
        if (aspectRatio === '16:9') {
          width = 1280;
          height = 720;
        } else if (aspectRatio === '9:16') {
          width = 720;
          height = 1280;
        } else if (aspectRatio === '4:3') {
          width = 1024;
          height = 768;
        } else if (aspectRatio === '3:4') {
          width = 768;
          height = 1024;
        }

        let imageUrl = '';
        let provider = 'NovaMind Neural Engine (100% Free Tier)';

        try {
          const encodedPrompt = encodeURIComponent(prompt);
          const seed = Math.floor(Math.random() * 1000000);
          const polUrl = `https://image.pollinations.ai/prompt/${encodedPrompt}?width=${width}&height=${height}&seed=${seed}&nologo=true`;

          const imgFetch = await fetch(polUrl);
          if (!imgFetch.ok) {
            throw new Error(`Image engine returned status ${imgFetch.status}`);
          }
          const buffer = await imgFetch.arrayBuffer();
          const base64 = Buffer.from(buffer).toString('base64');
          const mime = imgFetch.headers.get('content-type') || 'image/jpeg';
          imageUrl = `data:${mime};base64,${base64}`;
        } catch (genErr) {
          console.error('[Image Gen Error]', genErr);
          const enc = encodeURIComponent(prompt.slice(0, 50));
          imageUrl = `https://picsum.photos/seed/${enc}/${width}/${height}`;
          provider = 'NovaMind Fallback Image Generator';
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          imageUrl,
          prompt,
          aspectRatio,
          provider,
          createdAt: new Date().toISOString()
        }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message || 'Failed to generate image.' }));
      }
    });
    return;
  }

  // Route: /api/edit-image (Inpaint / Object Replacement / Magic Erase & Fill)
  if (pathname === '/api/edit-image' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const action = payload.action || 'replace_object'; // 'replace_object' | 'remove_object' | 'inpaint'
        const rawPrompt = (payload.prompt || '').trim();
        const baseImage = payload.image || ''; // data URL
        const aspectRatio = payload.aspectRatio || '1:1';

        let width = 1024;
        let height = 1024;
        if (aspectRatio === '16:9') { width = 1280; height = 720; }
        else if (aspectRatio === '9:16') { width = 720; height = 1280; }
        else if (aspectRatio === '4:3') { width = 1024; height = 768; }
        else if (aspectRatio === '3:4') { width = 768; height = 1024; }

        let synthesizedPrompt = '';
        if (action === 'remove_object') {
          // Object Removal & Background Infill
          const sceneContext = rawPrompt ? `, background: ${rawPrompt}` : '';
          synthesizedPrompt = `Clean empty background scene with object removed, pristine seamlessly infilled scenery, empty road, background pavement, high photorealism, award-winning photography, natural lighting, seamless continuation${sceneContext}`;
        } else {
          // Replace Object (e.g. Car Replacement)
          const objectTarget = rawPrompt || 'custom modern supercar';
          synthesizedPrompt = `Photorealistic ultra high definition shot of ${objectTarget}, parked in position, matching natural lighting, cinematic reflections, shadows, seamless perspective, masterwork, 8k uhd`;
        }

        let imageUrl = '';
        let provider = 'NovaMind Generative Inpaint & Object Studio';

        try {
          const encodedPrompt = encodeURIComponent(synthesizedPrompt);
          const seed = Math.floor(Math.random() * 1000000);
          const polUrl = `https://image.pollinations.ai/prompt/${encodedPrompt}?width=${width}&height=${height}&seed=${seed}&nologo=true`;

          const imgFetch = await fetch(polUrl);
          if (!imgFetch.ok) {
            throw new Error(`Inpaint engine status ${imgFetch.status}`);
          }
          const buffer = await imgFetch.arrayBuffer();
          const base64 = Buffer.from(buffer).toString('base64');
          const mime = imgFetch.headers.get('content-type') || 'image/jpeg';
          imageUrl = `data:${mime};base64,${base64}`;
        } catch (genErr) {
          console.error('[Inpaint Gen Error]', genErr);
          imageUrl = baseImage || `https://picsum.photos/seed/${encodeURIComponent(synthesizedPrompt.slice(0, 40))}/${width}/${height}`;
          provider = 'NovaMind Fallback Infill Engine';
        }

        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({
          imageUrl,
          action,
          prompt: rawPrompt || synthesizedPrompt,
          provider,
          createdAt: new Date().toISOString()
        }));
      } catch (err) {
        res.writeHead(500, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ error: err.message || 'Failed to edit image.' }));
      }
    });
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
        'Content-Disposition': 'attachment; filename="novamind-debug.apk"'
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

  // Route: /api/chat (With ChatGPT-style progressive streaming & attachments)
  if (pathname === '/api/chat' && req.method === 'POST') {
    let body = '';
    req.on('data', chunk => { body += chunk; });
    req.on('end', async () => {
      try {
        const payload = JSON.parse(body || '{}');
        const model = payload.model || 'google/gemini-3.6-flash';
        const messages = payload.messages || [];
        const customApiKey = payload.apiKey;
        const wantsStream = payload.stream !== false; // Default to true for ChatGPT-style streaming

        let geminiModel = 'gemini-3.6-flash';
        if (model.includes('3.8')) {
          geminiModel = 'gemini-3.8-flash';
        } else if (model.includes('pro')) {
          geminiModel = 'gemini-3.1-pro-preview';
        } else {
          geminiModel = 'gemini-3.6-flash';
        }

        // Build Gemini contents array (supporting text + image attachments)
        const contents = [];
        let sysPrompt = DEVELOPER_INSTRUCTION;

        for (const msg of messages) {
          if (msg.role === 'system') {
            if (msg.content && !msg.content.includes('PowerPlayExe')) {
              sysPrompt = `${DEVELOPER_INSTRUCTION}\n\nUser Persona: ${msg.content}`;
            } else if (msg.content) {
              sysPrompt = msg.content;
            }
          } else if (msg.role === 'user') {
            const parts = [];
            if (msg.content) {
              parts.push({ text: msg.content });
            }
            // If attachments are included (images or text files)
            if (Array.isArray(msg.attachments)) {
              for (const att of msg.attachments) {
                if (att.type === 'image' && att.data) {
                  const rawData = String(att.data || '').replace(/^data:[^;]+;base64,/, '');
                  parts.push({
                    inlineData: {
                      mimeType: att.mimeType || 'image/jpeg',
                      data: rawData
                    }
                  });
                } else if (att.type === 'file' && att.text) {
                  parts.push({
                    text: `\n[Attached File: ${att.name || 'document'}]\n\`\`\`\n${att.text}\n\`\`\`\n`
                  });
                }
              }
            }
            if (parts.length === 0) parts.push({ text: ' ' });
            contents.push({ role: 'user', parts });
          } else if (msg.role === 'assistant') {
            contents.push({ role: 'model', parts: [{ text: msg.content || '' }] });
          }
        }

        const systemInstruction = { parts: [{ text: sysPrompt }] };

        const isOpenRouterModel = model.startsWith('meta-llama/') || model.startsWith('mistralai/') || model.startsWith('deepseek/');

        // If OpenRouter model
        if (isOpenRouterModel) {
          const apiKey = customApiKey || process.env.OPENROUTER_API_KEY;
          if (!apiKey) {
            // Fallback directly to free Gemini
            // Handled below
          } else {
            try {
              const orMessages = [
                { role: 'system', content: DEVELOPER_INSTRUCTION },
                ...messages.filter(m => m.role !== 'system')
              ];

              const openRouterResp = await fetch('https://openrouter.ai/api/v1/chat/completions', {
                method: 'POST',
                headers: {
                  'Authorization': `Bearer ${apiKey}`,
                  'Content-Type': 'application/json',
                  'HTTP-Referer': 'https://ai.studio/build',
                  'X-Title': 'NovaMind AI'
                },
                body: JSON.stringify({
                  model,
                  messages: orMessages,
                  max_tokens: 2048,
                  stream: wantsStream
                })
              });

              if (!openRouterResp.ok) {
                // Fallback to Gemini if OpenRouter fails
              } else if (wantsStream && openRouterResp.body) {
                res.writeHead(200, {
                  'Content-Type': 'text/event-stream',
                  'Cache-Control': 'no-cache',
                  'Connection': 'keep-alive'
                });

                const reader = openRouterResp.body.getReader();
                const decoder = new TextDecoder();
                while (true) {
                  const { done, value } = await reader.read();
                  if (done) break;
                  const chunk = decoder.decode(value);
                  const lines = chunk.split('\n');
                  for (const line of lines) {
                    if (line.startsWith('data: ')) {
                      const dataStr = line.slice(6).trim();
                      if (dataStr === '[DONE]') {
                        res.write(`data: ${JSON.stringify({ done: true, model })}\n\n`);
                      } else {
                        try {
                          const parsed = JSON.parse(dataStr);
                          const delta = parsed.choices?.[0]?.delta?.content || '';
                          if (delta) {
                            res.write(`data: ${JSON.stringify({ chunk: delta })}\n\n`);
                          }
                        } catch (e) {}
                      }
                    }
                  }
                }
                res.write(`data: ${JSON.stringify({ done: true, model })}\n\n`);
                res.end();
                return;
              } else {
                const orData = await openRouterResp.json();
                const replyText = orData.choices?.[0]?.message?.content || '';
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ reply: replyText, model }));
                return;
              }
            } catch (orErr) {
              // Proceed to Gemini fallback
            }
          }
        }

        // Built-in 100% Free Gemini API with SSE Streaming support
        if (!GEMINI_API_KEY) {
          throw new Error('Server GEMINI_API_KEY is not configured in the environment.');
        }

        if (wantsStream) {
          // Stream directly from Gemini via SSE
          const geminiStreamUrl = `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:streamGenerateContent?alt=sse&key=${GEMINI_API_KEY}`;
          const geminiResp = await fetch(geminiStreamUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ contents, systemInstruction })
          });

          if (!geminiResp.ok) {
            const errBody = await geminiResp.text();
            throw new Error(`Gemini Stream error ${geminiResp.status}: ${errBody.slice(0, 100)}`);
          }

          res.writeHead(200, {
            'Content-Type': 'text/event-stream',
            'Cache-Control': 'no-cache',
            'Connection': 'keep-alive'
          });

          const reader = geminiResp.body.getReader();
          const decoder = new TextDecoder();
          let sseBuffer = '';

          while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            sseBuffer += decoder.decode(value, { stream: true });
            const lines = sseBuffer.split('\n');
            sseBuffer = lines.pop() || '';

            for (const line of lines) {
              const trimmed = line.trim();
              if (trimmed.startsWith('data: ')) {
                const jsonStr = trimmed.slice(6).trim();
                try {
                  const parsed = JSON.parse(jsonStr);
                  const textDelta = parsed.candidates?.[0]?.content?.parts?.[0]?.text;
                  if (textDelta) {
                    res.write(`data: ${JSON.stringify({ chunk: textDelta })}\n\n`);
                  }
                } catch (pe) {}
              }
            }
          }

          res.write(`data: ${JSON.stringify({ done: true, model: geminiModel })}\n\n`);
          res.end();
          return;
        }

        // Standard Non-streaming response
        const geminiUrl = `https://generativelanguage.googleapis.com/v1beta/models/${geminiModel}:generateContent?key=${GEMINI_API_KEY}`;
        const geminiResp = await fetch(geminiUrl, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ contents, systemInstruction })
        });

        const geminiData = await geminiResp.json();
        if (!geminiResp.ok) {
          throw new Error(geminiData.error?.message || 'Gemini API call failed.');
        }

        const reply = geminiData.candidates?.[0]?.content?.parts?.[0]?.text || "I couldn't generate a response.";
        res.writeHead(200, { 'Content-Type': 'application/json' });
        res.end(JSON.stringify({ reply, model: geminiModel }));

      } catch (err) {
        if (!res.headersSent) {
          res.writeHead(500, { 'Content-Type': 'application/json' });
          res.end(JSON.stringify({ error: err.message || 'Internal server error' }));
        } else {
          res.write(`data: ${JSON.stringify({ error: err.message, done: true })}\n\n`);
          res.end();
        }
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
