@Autowired
private RocketCalculator calculator;

@GetMapping("/test-core")
public Object testCore() {
    return calculator.calculateMinimalRocket();
}

