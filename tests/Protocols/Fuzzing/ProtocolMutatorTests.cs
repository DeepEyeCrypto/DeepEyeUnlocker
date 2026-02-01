using System;
using System.Linq;
using System.Text;
using Xunit;

namespace DeepEyeUnlocker.Tests.Protocols.Fuzzing
{
    public class ProtocolMutatorTests
    {
        [Fact]
        public void BitFlip_ChangesData()
        {
            var mutator = new ProtocolMutator(42);
            byte[] original = new byte[] { 0xAA, 0xBB, 0xCC, 0xDD };
            byte[] mutated = mutator.BitFlip(original);
            
            Assert.NotEqual(original, mutated);
            Assert.Equal(original.Length, mutated.Length);
        }

        [Fact]
        public void XMLMutation_HandlesXml()
        {
            var mutator = new ProtocolMutator(1234);
            string xml = "<?xml version=\"1.0\" ?><data><configure MemoryName=\"emmc\" /></data>";
            byte[] original = Encoding.UTF8.GetBytes(xml);
            
            // Try many times because strategy is random
            bool foundChange = false;
            for (int i = 0; i < 50; i++)
            {
                byte[] mutated = mutator.Mutate(original);
                if (!original.SequenceEqual(mutated))
                {
                    string mutatedStr = Encoding.UTF8.GetString(mutated);
                    if (mutatedStr != xml)
                    {
                        foundChange = true;
                        break;
                    }
                }
            }
            Assert.True(foundChange, "XML should have been mutated after 50 attempts.");
        }

        [Fact]
        public void Truncate_ShortensData()
        {
            var mutator = new ProtocolMutator(99);
            byte[] original = Enumerable.Range(0, 100).Select(x => (byte)x).ToArray();
            
            bool foundTruncation = false;
            for (int i = 0; i < 100; i++)
            {
                byte[] mutated = mutator.TruncateOrExtend(original);
                if (mutated.Length < original.Length)
                {
                    foundTruncation = true;
                    break;
                }
            }
            Assert.True(foundTruncation);
        }
    }
}
