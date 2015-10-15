class GraphQlController < ApplicationController
  skip_before_filter :verify_authenticity_token

  def query
    binding.pry
  end

  def mutation
    binding.pry
  end
end
