// Display only. All real money maths happens on the server, in cents.
export const rs = (cents) =>
  `Rs. ${(cents / 100).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
