from azure.identity import DefaultAzureCredential, get_bearer_token_provider
import openai
import os
import requests
import logging

# Returns a +ve number if successful
# It sets openai with the endpoint, api key etc.
# TODO: stop dumping exception into stdout
def setupOpenai(aoai_endpoint, aoai_version):
  openai.azure_endpoint = aoai_endpoint
  openai.api_version = aoai_version
  try:
    api_key = os.getenv("OPENAI_API_KEY")
    if api_key and not api_key.isspace():
      # the string is non-empty
      openai.api_type = "azure"
      openai.api_key = api_key
      print("\nGot OPENAI API Key from environment variable")
      logging.info("\nGot OPENAI API Key from environment variable")
      return 1
    else:
      # the string is empty
      try:
        print(f"\nCould not get API key from environment variable OPENAI_API_KEY. Trying Managed ID")
        logging.info(f"\nCould not get API key from environment variable OPENAI_API_KEY. Trying Managed ID")
        
        # default_credential = DefaultAzureCredential()
        # token = default_credential.get_token("https://cognitiveservices.azure.com/.default")
        # openai.api_type = "azure_ad"
        # openai.api_key = token.token
        
        # https://learn.microsoft.com/en-us/azure/ai-services/openai/how-to/managed-identity
        token_provider = get_bearer_token_provider(  DefaultAzureCredential(), "https://cognitiveservices.azure.com/.default")
        openai.azure_ad_token_provider = token_provider
        
        print("\nAuthenticated successfully with AAD token")
        logging.info("\nAuthenticated successfully with AAD token")
        return 2
      except:
        print("\nSomething went wrong getting token with Managed Identity")
        logging.info("\nSomething went wrong getting token with Managed Identity")
        return -2
  except:
    print("\nSomething went wrong getting access key from environment variable")
    logging.info("\nSomething went wrong getting access key from environment variable")
    return -1

# Returns total tokens used and the chat completion
def getChatCompletion(the_engine, the_messages):
    response = openai.chat.completions.create(        
        model=the_engine,
        messages=the_messages,
        temperature=0,
        max_tokens=1000,
        #top_p=1,
        frequency_penalty=0,
        presence_penalty=0,
        stop=None
    )
    # https://stackoverflow.com/questions/77444332/openai-python-package-error-chatcompletion-object-is-not-subscriptable
    return response.usage.total_tokens, response.choices[0].finish_reason, response.choices[0].message.content

# Return vectors for a given text using the ADA model
def generate_embedding(the_engine, the_text):
    response = openai.Embedding.create(
                  input=the_text, 
                  engine=the_engine)
    embedding = response['data'][0]['embedding']
    return embedding
