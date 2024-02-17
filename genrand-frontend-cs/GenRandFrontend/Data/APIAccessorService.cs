namespace GenRandFrontend.Data
{
    public class APIAccessorService
    {
        private const string API_LOCATION = "http://localhost:8080";
        private readonly HttpClient client;

        public APIAccessorService()
        {
            client = new HttpClient();
        }

        public async Task<ARXMixEntry> GetARXMixByDefinition(string type, ulong packedDefinition)
        {
            List<ARXMixEntry>? value = (List<ARXMixEntry>?) await client.GetFromJsonAsync($"{API_LOCATION}/arx/{type}/definition/{packedDefinition}",typeof(List<ARXMixEntry>));
            if (value == null)
            {
                // error case... handle later
                throw new Exception($"value returned null for {type} with {packedDefinition}");
            }
            if (value.Count == 0)
            {
				throw new Exception($"value not present for {type} with {packedDefinition}");
			}
            return value[0];
        }
    }
}
