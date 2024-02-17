using System.Text;
using System.Text.Json.Serialization;

namespace GenRandFrontend.Data
{
	/// <summary>
	/// Contains info about an ARX-based mix function that the API will return. Based on the API-side class of the same name.
	/// The fields have identical meanings.
	/// </summary>
	public class ARXMixEntry
	{
		public string Type { get; }
		public long Definition { get; }
		public double AvScore1 { get; }
		public double AvScore2 { get; }
		public double AvScore3 { get; }
		public double AvScore4 { get; }
		public string AvImage1 { get; }
		public string AvImage2 { get; }
		public string AvImage3 { get; }
		public string AvImage4 { get; }

		[JsonConstructor]
		public ARXMixEntry(string type, long definition, double avScore1, double avScore2, double avScore3, double avScore4, string avImage1, string avImage2, string avImage3, string avImage4)
		{
			Type = type;
			Definition = definition;
			AvScore1 = avScore1;
			AvScore2 = avScore2;
			AvScore3 = avScore3;
			AvScore4 = avScore4;
			AvImage1 = avImage1;
			AvImage2 = avImage2;
			AvImage3 = avImage3;
			AvImage4 = avImage4;
		}

		public override string ToString()
		{
			return $"{Type},{Definition},{AvScore1},{AvScore2},{AvScore3},{AvScore4},{AvImage1},{AvImage2},{AvImage3},{AvImage4}";
		}
	}
}
