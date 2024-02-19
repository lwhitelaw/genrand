using Microsoft.AspNetCore.Mvc.ModelBinding.Validation;
using System.Diagnostics.Eventing.Reader;
using System.Runtime.CompilerServices;
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
		/// <summary>
		/// The type string of this mix: one of 8x2, 8x3, 8x4, 16x2, 16x3, 16x4, 32x2, 32x3, 32x4.
		/// Other values may be supported later.
		/// </summary>
		public string Type { get; }
		/// <summary>
		/// The definition of this mix as packed into a long and returned by the API.
		/// Use DecodeDefinition to get the values.
		/// </summary>
		public long Definition { get; }
		/// <summary>
		/// Avalanche score when function is run only once.
		/// </summary>
		public double AvScore1 { get; }
		/// <summary>
		/// Avalanche score when function is run in two rounds.
		/// </summary>
		public double AvScore2 { get; }
		/// <summary>
		/// Avalanche score when function is run in three rounds.
		/// </summary>
		public double AvScore3 { get; }
		/// <summary>
		/// Avalanche score when function is run in four rounds.
		/// </summary>
		public double AvScore4 { get; }
		/// <summary>
		/// Hex ID string for avalanche graph for one round.
		/// </summary>
		public string AvImage1 { get; }
		/// <summary>
		/// Hex ID string for avalanche graph for two rounds.
		/// </summary>
		public string AvImage2 { get; }
		/// <summary>
		/// Hex ID string for avalanche graph for three rounds.
		/// </summary>
		public string AvImage3 { get; }
		/// <summary>
		/// Hex ID string for avalanche graph for four rounds.
		/// </summary>
		public string AvImage4 { get; }

		/// <summary>
		/// Construct an entry explicitly.
		/// </summary>
		/// <param name="type">Mix type from the supported ones</param>
		/// <param name="definition">Packed definition</param>
		/// <param name="avScore1">1 round score</param>
		/// <param name="avScore2">2 rounds score</param>
		/// <param name="avScore3">3 rounds score</param>
		/// <param name="avScore4">4 rounds score</param>
		/// <param name="avImage1">1 round graph reference</param>
		/// <param name="avImage2">2 round graph reference</param>
		/// <param name="avImage3">3 round graph reference</param>
		/// <param name="avImage4">4 round graph reference</param>
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

		/// <summary>
		/// All mix types recognised.
		/// </summary>
		private static readonly string[] TYPES = new string[]
		{
			"8x2", "8x3", "8x4",
			"16x2", "16x3", "16x4",
			"32x2", "32x3", "32x4",
			"64x2", "64x3", "64x4"
		};
		/// <summary>
		/// Rotation bits needed for mix types.
		/// </summary>
		private static readonly int[] ROT_BITS = new int[] { 
			3, 3, 3, // 8x2, 8x3, 8x4
			4, 4, 4, // 16x2, 16x3, 16x4
			5, 5, 5, // 32x2, 32x3, 32x4
			6, 6, 6 // 64x2, 64x3, 64x4
		};
		/// <summary>
		/// Number of operators in a mix type.
		/// </summary>
		private static readonly int[] OPERATORS = new int[] { 
			4, 6, 8, // 8x2, 8x3, 8x4
			4, 6, 8, // 16x2, 16x3, 16x4
			4, 6, 8, // 32x2, 32x3, 32x4
			4, 6, 8 // 64x2, 64x3, 64x4
		};
		/// <summary>
		/// Number of terms used in a mix type.
		/// </summary>
		private static readonly int[] TERMS = new int[] {
			2, 3, 4, // 8x2, 8x3, 8x4
			2, 3, 4, // 16x2, 16x3, 16x4
			2, 3, 4, // 32x2, 32x3, 32x4
			2, 3, 4 // 64x2, 64x3, 64x4
		};
		/// <summary>
		/// Return index into the above arrays for a given mix type string.
		/// </summary>
		/// <param name="type">the type to lookup</param>
		/// <returns>The index, or -1 if the type is not recognised</returns>
		private static int LookupInfoByTypeString(string type)
		{
			for (int i = 0; i < TYPES.Length; i++) {
				if (TYPES[i].Equals(type)) { return i; }
			}
			return -1;
		}
		/// <summary>
		/// Decode a packed definition with the given type into the rotation and xors arrays.
		/// </summary>
		/// <param name="type">type to decode with</param>
		/// <param name="definition">definition to decode</param>
		/// <param name="rotations">rotations are stored here</param>
		/// <param name="xors">xor/add booleans are stored here</param>
		/// <exception cref="ArgumentException"></exception>
		public void DecodeDefinition(string type, long definition, out int[] rotations, out bool[] xors)
		{
			ulong def = (ulong)definition;
			int typeIndex = LookupInfoByTypeString(type);
			if (typeIndex == -1)
			{
				throw new ArgumentException("Type is not known");
			}
			int operators = OPERATORS[typeIndex];
			int bits = ROT_BITS[typeIndex];
			rotations = new int[operators];
			xors = new bool[operators];
			// unpack value - note indices are in reverse order
			// write order wrote in xor bools followed by rotations a,b,c,d, etc. in that order
			// therefore must unpack in reverse

			// unpack rotations
			for (int i = operators-1; i >= 0; i--)
			{
				// mask off needed bits and cast down to write in
				rotations[i] = (int)(def & ((1U << bits) - 1));
				// shift definition bits over
				def = (def >> bits);
			}

			// unpack xors
			for (int i = operators - 1; i >= 0; i--)
			{
				// mask off needed bits and cast down to write in
				xors[i] = (def & 0x1) == 0x1;
				// shift definition bits over
				def = (def >> 1);
			}
		}

		/// <summary>
		/// Return a terse string describing this function suitable for list displays
		/// </summary>
		/// <returns>terse description of this mix</returns>
		public string GetTerseOperationString()
		{
			StringBuilder sb = new StringBuilder();
			int[] rotations;
			bool[] xors;
			DecodeDefinition(Type, Definition, out rotations, out xors);
			for (int i = 0; i < rotations.Length; i++)
			{
				sb.Append(xors[i] ? "XOR " : "ADD ");
				sb.Append(rotations[i]);
				if (i != rotations.Length-1) sb.Append(" ");
			}
			return sb.ToString();
		}

		/// <summary>
		/// Return a string describing this mix function with (generic C-like) code.
		/// </summary>
		/// <returns>a string for detailed display</returns>
		public string GetCodeString()
		{
			// Single-letter variable names to use in generated code
			// as a Feistel network the code always starts from a and reads the last term first
			// before rotating to the next value in order
			const string variables = "abcd";
			StringBuilder sb = new StringBuilder();
			int[] rotations;
			bool[] xors;
			DecodeDefinition(Type, Definition, out rotations, out xors);
			int terms = TERMS[LookupInfoByTypeString(Type)];
			// index of variable written into - always "a"
			int receiverIndex = 0;
			// index of variable that is read - is the last term depending on term count, "b" for x2, "c" x3, "d" x4
			int argumentIndex = terms - 1;
			// for each operator, write out a line depending on whether operator is XOR or ADD
			for (int i = 0; i < rotations.Length; i++)
			{
				if (xors[i])
				{
					sb.Append($"{variables[receiverIndex]} ^= ROTL({variables[argumentIndex]},{rotations[i]});\n");
				}
				else
				{
					sb.Append($"{variables[receiverIndex]} += ROTL({variables[argumentIndex]},{rotations[i]});\n");
				}
				// roll the variable counters
				receiverIndex++; if (receiverIndex == terms) receiverIndex = 0;
				argumentIndex++; if (argumentIndex == terms) argumentIndex = 0;
			}
			return sb.ToString();
		}

		const string IMAGE_PATH_PREFIX = "/images";

		public static string GetImagePath(string imageRefHex)
		{
			ulong hexValue = Convert.ToUInt64(imageRefHex, 16);
			return string.Format("{0}/{1:X3}/{2}.png", IMAGE_PATH_PREFIX, Mix12Bits(hexValue), imageRefHex);
		}

		private static int Mix12Bits(ulong v)
		{
			v ^= v >> 21;
			v *= 0x2AE264A9B1A36D69UL;
			v ^= v >> 37;
			v *= 0x396747CA3A58E56FUL;
			v ^= v >> 44;
			v *= 0xFB7719182775D593UL;
			v ^= v >> 21;
			return (int)(v & 0xFFFUL);
		}

	}
}
