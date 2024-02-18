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

		private static readonly string[] TYPES = new string[]
		{
			"8x2", "8x3", "8x4",
			"16x2", "16x3", "16x4",
			"32x2", "32x3", "32x4",
			"64x2", "64x3", "64x4"
		};
		private static readonly int[] ROT_BITS = new int[] { 
			3, 3, 3, // 8x2, 8x3, 8x4
			4, 4, 4, // 16x2, 16x3, 16x4
			5, 5, 5, // 32x2, 32x3, 32x4
			6, 6, 6 // 64x2, 64x3, 64x4
		};
		private static readonly int[] TERMS = { 
			4, 6, 8, // 8x2, 8x3, 8x4
			4, 6, 8, // 16x2, 16x3, 16x4
			4, 6, 8, // 32x2, 32x3, 32x4
			4, 6, 8 // 64x2, 64x3, 64x4
		};

		private static int LookupInfoByTypeString(string type)
		{
			for (int i = 0; i < TYPES.Length; i++) {
				if (TYPES[i].Equals(type)) { return i; }
			}
			return -1;
		}
		public void DecodeDefinition(string type, long definition, out int[] rotations, out bool[] xors)
		{
			ulong def = (ulong)definition;
			int typeIndex = LookupInfoByTypeString(type);
			if (typeIndex == -1)
			{
				throw new ArgumentException("Type is not known");
			}
			int terms = TERMS[typeIndex];
			int bits = ROT_BITS[typeIndex];
			rotations = new int[terms];
			xors = new bool[terms];
			// unpack value - note indices are in reverse order
			// write order wrote in xor bools followed by rotations a,b,c,d, etc. in that order
			// therefore must unpack in reverse

			// unpack rotations
			for (int i = terms-1; i >= 0; i--)
			{
				// mask off needed bits and cast down to write in
				rotations[i] = (int)(def & ((1U << bits) - 1));
				// shift definition bits over
				def = (def >> bits);
			}

			// unpack xors
			for (int i = terms - 1; i >= 0; i--)
			{
				// mask off needed bits and cast down to write in
				xors[i] = (def & 0x1) == 0x1;
				// shift definition bits over
				def = (def >> 1);
			}
		}

		public string GetTerseOperationString()
		{
			StringBuilder sb = new StringBuilder();
			int[] rotations;
			bool[] xors;
			DecodeDefinition(Type, Definition, out rotations, out xors);
			for (int i = 0; i < rotations.Length; i++)
			{
				sb.Append(xors[i] ? "XOR " : "ADD ");
				sb.Append("ROTL(").Append(rotations[i]).Append(")");
				if (i != rotations.Length-1) sb.Append(" ");
			}
			return sb.ToString();
		}
	}
}
