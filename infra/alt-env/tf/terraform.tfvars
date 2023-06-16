second_level_domain_name = "conjob"
top_level_domain_name = "io"
subdomain_name = "alt"
instance_type = "t3.medium"
spot_type = "persistent"
instance_interruption_behavior = "stop"
ami = "ami-0d9fad4f90eb14fc3" # Canonical, Ubuntu, 22.04 LTS, amd64 jammy image build on 2023-04-28
spot_price = "0.023"
volume_size = 30
public_key = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApwf7mMF+u44OxPJS3ivDit2Cb2RoGb9yDjosgJlHCX6v4qEf764pkg7zQ4MhhXO9He9SYrW/gUpR1rnA+lWXCGc8HPXdz3F9RWJ2vtvZOJiE9JSz6+TRA2Xu+obyNs9On6uNhlz2Z4/3pbiNBbsL4RHyu+je9umS5RyKleD9S4OR5ekk+u7oTVHyUJBmmlqaH5VCoWXQa2HrQAgGg/LcuQSp7ceQP62X1pHO4Ty0sY8Im8kC5BVRAd3NaHPFbjDqdlCzNZchmdgXimAQB5wXLjqEwszp76Ak1mESqecqSuEHL5bQDSloJ1usT7w00Tx2z6oJQgVAj9YM6f5Y3f92EwIDAQAB"
acme_server_url = "https://acme-staging-v02.api.letsencrypt.org/directory"
