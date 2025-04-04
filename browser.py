## This class could be replaced with urllib

import socket
import ssl
import base64
from http.client import HTTPResponse
from io import BytesIO

##This class is a URL parser that returns an object URL
class URL:
    MAX_REDIRECTS = 5
    _shared_sockets = {}

    def __init__ (self, url):   
        self.raw_url = url

        if url.startswith("view-source:"):
            self.scheme = "view-source"
            self.view_source_url = url[len("view-source:"):]
            self.parsed_url = URL(self.view_source_url)
            return

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

    def request(self, redirects_left = MAX_REDIRECTS):
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
        elif self.scheme == "view-source":
            return self.parsed_url.request()
        else:
            key = (self.host, self.port)
            if key in URL._shared_sockets:
                s = URL._shared_sockets[key]
            else:
                s = socket.socket(
                    family = socket.AF_INET,
                    type = socket.SOCK_STREAM,
                    proto = socket.IPPROTO_TCP
                )
                s.connect(key)
            
                if self.scheme == "https":
                    ctx = ssl.create_default_context()
                    s = ctx.wrap_socket(s, server_hostname=self.host)
                URL._shared_sockets[key] = s

            request = f"GET {self.path} HTTP/1.1\r\n"
            request += f"Host: {self.host}\r\n"
            request += "User-Agent: PythonBrowser/0.1\r\n"
            request += "Connection: keep-alive\r\n" # Only need in HTTP/1.1 
            request += "\r\n"
            s.send(request.encode("utf-8"))

            raw_response = s.makefile("rb")
            response = HTTPResponse(raw_response)
            response.begin()

            status = response.status
            reason = response.reason
            version = response.version     

            response_headers = {}       
            
            for header, value in response.getheaders():
                response_headers[header.casefold()] = value.strip()

            if 300 <= status < 400:
                if not redirects_left:
                    raise Exception("Too many redirects")
                location = response_headers.get("location")
                if not location:
                    raise Exception(f"Redirect without location header: {status} {reason}")
                
                redirect_url = location
                if not redirect_url.startswith("http"):
                    #Handle relative URL
                    redirect_url = f"{self.scheme}://{self.host}"
                    if not redirect_url.endswith("/") and not location.startswith("/"):
                        redirect_url += "/"
                    redirect_url += location
                
                print("Redirecting to:", redirect_url)
                return URL(redirect_url).request(redirects_left=redirects_left-1)

            else:
                content_length = response_headers.get("content-length")
                if content_length:
                    content_length = int(content_length)
                    content = response.read(content_length).decode("utf-8")
                else:
                    content = response.read().decode("utf-8")

                if response.getheader("Connection") != "close":
                    return content
                else:
                    del URL._shared_sockets[key]
                    s.close()
                    return content            
    
    def show(body):
        in_tag = False
        entity_buffer = ""
        for c in body:
            if in_tag:
                if c ==">":
                    in_tag = False
                continue
            elif c == "<":
                in_tag = True
            elif c == "&":
                entity_buffer = "&"
            elif entity_buffer == "&":
                if c == "l":
                    entity_buffer += c
                elif c == "g":
                    entity_buffer += c
                else:
                    print(entity_buffer, end="") #Print if isn't part of an entity
                    print(c, end="")
                    entity_buffer
            elif entity_buffer == "&l":
                if c == "t":
                    entity_buffer += c
                else:
                    print(entity_buffer, end="") #Print if isn't part of an entity
                    print(c, end="")
                    entity_buffer = ""
            elif entity_buffer == "&lt":
                if c == ";":
                    print("<", end="")
                    entity_buffer = ""
                else:
                    print(entity_buffer, end="") #Print if isn't part of an entity
                    print(c, end="")
                    entity_buffer = ""
            elif entity_buffer == "&g":
                if c == "t":
                    entity_buffer += c
                else:
                    print(entity_buffer, end="") #Print if isn't part of an entity
                    print(c, end="")
                    entity_buffer = ""
            elif entity_buffer == "&gt":
                if c == ";":
                    print(">", end="")
                    entity_buffer = ""
                else:
                    print(entity_buffer, end="") #Print if isn't part of an entity
                    print(c, end="")   
                    entity_buffer = ""
            else:
                print(c, end="")
        
        if entity_buffer:
            print(entity_buffer, end="")
            

    def load(self):
        if self.scheme == "view-source":
            body = self.request()
            print(body)
        else:
            body = self.request()
            URL.show(body)

if __name__ == "__main__":
    import sys
    url = URL(sys.argv[1])
    url.load()
        