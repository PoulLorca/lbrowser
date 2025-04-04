import unittest
import io
import sys
from browser import URL

class TestURL(unittest.TestCase):
    def test_data_url_html(self):
        # Test the data URL with plain text
        dataurl = "data:text/html,<h1>Hello, World!</h1>"
        url = URL(dataurl)
        captured_output = io.StringIO()
        sys.stdout = captured_output
        url.load()
        sys.stdout = sys.__stdout__        
        result = captured_output.getvalue().strip()
        expected = "Hello, World!"
        self.assertEqual(result, expected)

    def test_data_url_plain_base64(self):
        # Test the data URL with base64 encoded plain text
        dataurl = "data:text/plain;base64,SGVsbG8sIFdvcmxkIQ=="
        url = URL(dataurl)
        captured_output = io.StringIO()
        sys.stdout = captured_output
        url.load()
        sys.stdout = sys.__stdout__        
        result = captured_output.getvalue().strip()
        expected = "Hello, World!"
        self.assertEqual(result, expected)

    def test_data_entities(self):
        # Test the data URL with entities
        dataurl = "data:text/html,<h1>This text contains a &lt;strong&gt;text&lt;/strong&gt; and a &gt; char.</h1>"
        url = URL(dataurl)
        captured_output = io.StringIO()
        sys.stdout = captured_output
        url.load()
        sys.stdout = sys.__stdout__
        result = captured_output.getvalue().strip()
        expected = "This text contains a <strong>text</strong> and a > char."
        self.assertEqual(result, expected)
    

if __name__ == "__main__":
    unittest.main()