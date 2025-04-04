## This class could be replaced with urllib

import socket
import ssl
import base64

##This class is a URL parser that returns an object URL
class URL:
    def __init__ (self, url):   
        self.raw_url = url
        if url.startswith("data:"):
            self.scheme = "data"
            _, self.data_content = url.split(":", 1)
            return             

        self.scheme, url = url.split('://',1)
        assert self.scheme in ['http', 'https', 'file']

        if self.scheme == "file":
            self.path = url if url.startswith("/") else "/" + url
            return

        if "/" in url:
            self.host, path = url.split('/', 1)            
            self.path = "/" + path
        else:
            self.host = url
            self.path = "/"

        if self.scheme == "http":
            self.port = 80
        elif self.scheme == "https":
            self.port = 443            

        if ":" in self.host:
            self.host, port = self.host.split(':', 1)
            self.port = int(port)

    def request(self):
        if self.scheme == "file":
            try:
                with open(self.path, encoding="utf-8") as f:
                    return f.read()
            except IOError as e:
                return f"Error opening file: {e}"
        elif self.scheme == "data":
            parts = self.data_content.split(",", 1)
            metadata = parts[0]
            data = parts[1]
            if ";base64" in metadata:
                return base64.b64decode(data).decode("utf-8")
            else:
                return data
        else:
            s = socket.socket(
                family = socket.AF_INET,
                type = socket.SOCK_STREAM,
                proto = socket.IPPROTO_TCP
            )
            s.connect((self.host, self.port))
            if self.scheme == "https":
                ctx = ssl.create_default_context()
                s = ctx.wrap_socket(s, server_hostname=self.host)

            request = f"GET {self.path} HTTP/1.1\r\n"
            request += f"Host: {self.host}\r\n"
            request += "User-Agent: PythonBrowser/0.1\r\n"
            request += "Connection: close\r\n" # Only need in HTTP/1.1 
            request += "\r\n"
            s.send(request.encode("utf-8"))

            response = s.makefile("r", encoding="utf-8", newline="\r\n")

            statusline = response.readline()
            version, status, explanation = statusline.split(" ", 2)

            response_headers = {}
            while True:
                line = response.readline()
                if line == "\r\n": 
                    break
                header, value = line.split(":", 1)
                response_headers[header.casefold()] = value.strip()
                #Avoided to try to load chuncked headers
                #assert "transfer-encoding" not in response_headers
                #assert "content-enconding" not in response_headers

            content = response.read()
            s.close()

            return content
    
    def show(body):
        in_tag = False
        for c in body:
            if c =="<":
                in_tag = True
            elif c == ">":
                in_tag = False
            elif not in_tag:
                print(c, end="")

    def load(self):
        body = self.request()
        URL.show(body)

if __name__ == "__main__":
    import sys
    url = URL(sys.argv[1])
    url.load()
        