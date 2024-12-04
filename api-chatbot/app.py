from quart import Quart, request, jsonify
import google.generativeai as genai
import os

app = Quart(__name__)

# Configure sua chave de API do Gemini
genai.configure(api_key=os.getenv('AIzaSyDNl7RYhfhe-tNZdjn1wkKhOkS2v0FUHXo'))

# Endpoint para o chat
@app.route('/chat', methods=['POST'])
async def chat():
    data = await request.get_json()
    pergunta = data.get('pergunta')
    
    if not pergunta:
        return jsonify({"erro": "A pergunta é obrigatória"}), 400

    resposta = await obter_resposta(pergunta)
    return jsonify({"resposta": resposta})

# Função para gerar a resposta da IA usando o modelo Gemini
async def obter_resposta(pergunta):
    try:
        # Configura o modelo para geração de resposta com a API do Gemini
        response = genai.Completion.create(
            model="gemini-1.5-flash",  # Ajuste o nome do modelo conforme necessário
            prompt=pergunta,
            max_output_tokens=150  # Limita a quantidade de tokens gerados na resposta
        )
        
        # A resposta da API estará dentro do campo 'choices', e o texto estará em 'text'
        return response['choices'][0]['text'].strip()

    except Exception as e:
        print(f"Erro ao acessar a API Gemini: {e}")
        return "Desculpe, ocorreu um erro ao processar sua pergunta."

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5000))
    app.run(port=port, debug=True)
