using Microsoft.AspNetCore.Http.Connections;

namespace GenRandFrontend.Data
{
	/// <summary>
	/// Accessor for the internal GenRand API
	/// </summary>
	public class APIAccessorService
	{
		/// <summary>
		/// Bind port for the GenRand API
		/// </summary>
		private const string API_LOCATION = "http://localhost:8080";
		private readonly HttpClient client;

		public APIAccessorService()
		{
			client = new HttpClient();
		}

		/// <summary>
		/// Return one value depending on type and packed definition.
		/// </summary>
		/// <param name="type">type to get</param>
		/// <param name="packedDefinition">packed definition to get</param>
		/// <returns>the requested mix</returns>
		/// <exception cref="Exception">if the mix is not present or getting it fails</exception>
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

		public async Task<long> GetARXCountByType(string type)
		{
			try
			{
				string value = await client.GetStringAsync($"{API_LOCATION}/arx/{type}/count");
				return long.Parse(value);
			}
			catch (HttpRequestException ex)
			{
				throw new Exception($"network problem accessing {type}",ex);
			}
			catch (FormatException ex)
			{
				throw new Exception($"could not parse return value for {type} as long", ex);
			}
		}

		public async Task<List<ARXMixEntry>> GetARXByTypeChronologically(string type, int page)
		{
			List<ARXMixEntry>? value = (List<ARXMixEntry>?) await client.GetFromJsonAsync($"{API_LOCATION}/arx/{type}/list/{page}", typeof(List<ARXMixEntry>));
			if (value == null)
			{
				throw new Exception($"value returned null for {type} page {page}");
			}
			return value;
		}

		public async Task<List<ARXMixEntry>> GetARXByTypeSortedByAvScore(string type, int round, int page)
		{
			string[] ROUNDS = { "unused", "round1", "round2", "round3", "round4" };
			if (round < 1 || round > 4) throw new ArgumentException("invalid number of rounds");
			List<ARXMixEntry>? value = (List<ARXMixEntry>?)await client.GetFromJsonAsync($"{API_LOCATION}/arx/{type}/topScoring/{ROUNDS[round]}/{page}", typeof(List<ARXMixEntry>));
			if (value == null)
			{
				throw new Exception($"value returned null for {type} page {page}");
			}
			return value;
		}
	}
}
