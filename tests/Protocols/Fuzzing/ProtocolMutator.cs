using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;

namespace DeepEyeUnlocker.Tests.Protocols.Fuzzing
{
    public class ProtocolMutator
    {
        private readonly Random _rng;

        public ProtocolMutator(int seed)
        {
            _rng = new Random(seed);
        }

        public byte[] Mutate(byte[] data)
        {
            if (data == null || data.Length == 0) return data;

            // Choose a random strategy
            int strategy = _rng.Next(100);

            if (data.Length > 10 && IsXml(data))
            {
                if (strategy < 60) return MutateXml(data); // XML specific mutation
            }

            if (strategy < 30) return BitFlip(data);
            if (strategy < 60) return ByteFlip(data);
            if (strategy < 80) return CorruptInteger(data);
            return TruncateOrExtend(data);
        }

        private bool IsXml(byte[] data)
        {
            try
            {
                string s = Encoding.UTF8.GetString(data);
                return s.TrimStart().StartsWith("<");
            }
            catch { return false; }
        }

        public byte[] BitFlip(byte[] data)
        {
            byte[] mutated = (byte[])data.Clone();
            int bitIdx = _rng.Next(mutated.Length * 8);
            mutated[bitIdx / 8] ^= (byte)(1 << (bitIdx % 8));
            return mutated;
        }

        public byte[] ByteFlip(byte[] data)
        {
            byte[] mutated = (byte[])data.Clone();
            mutated[_rng.Next(mutated.Length)] = (byte)_rng.Next(256);
            return mutated;
        }

        public byte[] CorruptInteger(byte[] data)
        {
            if (data.Length < 4) return data;
            byte[] mutated = (byte[])data.Clone();
            int offset = _rng.Next(mutated.Length - 3);
            
            // Choose between 2-byte or 4-byte corruption
            if (_rng.Next(2) == 0)
            {
                // 2-byte
                ushort val = (ushort)_rng.Next(ushort.MaxValue);
                BitConverter.TryWriteBytes(mutated.AsSpan(offset), val);
            }
            else
            {
                // 4-byte
                uint val = (uint)_rng.Next();
                BitConverter.TryWriteBytes(mutated.AsSpan(offset), val);
            }
            return mutated;
        }

        public byte[] TruncateOrExtend(byte[] data)
        {
            if (_rng.Next(2) == 0 && data.Length > 1)
            {
                // Truncate
                int newLen = _rng.Next(1, data.Length);
                return data.Take(newLen).ToArray();
            }
            else
            {
                // Extend
                byte[] extra = new byte[_rng.Next(1, 16)];
                _rng.NextBytes(extra);
                return data.Concat(extra).ToArray();
            }
        }

        private byte[] MutateXml(byte[] data)
        {
            string xml = Encoding.UTF8.GetString(data);
            
            // Strategies for XML:
            // 1. Corrupt attribute value
            // 2. Corrupt tag name
            // 3. Insert malformed tag
            // 4. Corrupt closing tag
            
            int subStrategy = _rng.Next(4);
            switch (subStrategy)
            {
                case 0: // Attribute value
                    var attrMatch = Regex.Match(xml, "=\"[^\"]*\"");
                    if (attrMatch.Success)
                    {
                        xml = xml.Substring(0, attrMatch.Index + 2) + "FUZZED_" + _rng.Next() + xml.Substring(attrMatch.Index + attrMatch.Length - 1);
                    }
                    break;
                case 1: // Tag name
                    var tagMatch = Regex.Match(xml, "<[a-zA-Z]+");
                    if (tagMatch.Success)
                    {
                        xml = xml.Substring(0, tagMatch.Index + 1) + "crashme" + xml.Substring(tagMatch.Index + tagMatch.Length);
                    }
                    break;
                case 2: // Malformed XML structure
                    xml = xml.Insert(_rng.Next(xml.Length), "<<//>>");
                    break;
                case 3: // Null bytes inside XML
                    int idx = _rng.Next(xml.Length);
                    xml = xml.Insert(idx, "\0\0\0");
                    break;
            }

            return Encoding.UTF8.GetBytes(xml);
        }
    }
}
